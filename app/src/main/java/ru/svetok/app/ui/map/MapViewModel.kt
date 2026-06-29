package ru.svetok.app.ui.map

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.svetok.app.data.geo.GeoJsonRepository
import ru.svetok.app.data.geo.StreetFeature
import ru.svetok.app.data.outage.ConnectivityMonitor
import ru.svetok.app.data.outage.MapOutage
import ru.svetok.app.data.outage.OutageLoadResult
import ru.svetok.app.data.outage.OutageMapStatus
import ru.svetok.app.data.outage.OutageRepository
import ru.svetok.app.data.outage.OutageSource

class MapViewModel(
    private val geoJsonRepository: GeoJsonRepository,
    private val outageRepository: OutageRepository,
    private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var cachedStreets: List<StreetFeature>? = null
    private var latestOutageLoad: OutageLoadResult? = null
    private var startupJob: Job? = null
    private var isRefreshing = false
    private var isOnline = false
    private var lastUpdatedMs: Long? = null
    private var connectivityJob: Job? = null

    init {
        observeCachedOutages()
        loadStreets()
        startStartupRetrySequence()
        observeConnectivity()
    }

    fun loadStreets() {
        val previousState = _uiState.value
        _uiState.value = previousState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            runCatching {
                cachedStreets ?: async(Dispatchers.IO) {
                    geoJsonRepository.loadStreetFeatures()
                }.await().also { cachedStreets = it }
            }.onSuccess {
                renderState(outageLoad = latestOutageLoad, isLoading = latestOutageLoad == null)
            }.onFailure { error ->
                Log.e(TAG, "Failed to load streets", error)
                _uiState.value = previousState.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Не удалось загрузить карту города.",
                )
            }
        }
    }

    fun onStreetTapped(streetNorm: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedStreet = buildSelectedStreet(
                streetNorm = streetNorm,
                streets = state.streets,
                outages = state.outages,
                highlightByStreetNorm = state.highlightByStreetNorm,
            ),
        )
    }

    fun clearStreetSelection() {
        _uiState.value = _uiState.value.copy(selectedStreet = null)
    }

    fun refreshNow() {
        viewModelScope.launch { refreshFromNetwork(showLoading = true) }
    }

    private fun observeCachedOutages() {
        viewModelScope.launch {
            outageRepository.observeCachedOutages().collect { cached ->
                cached ?: return@collect
                latestOutageLoad = cached
                renderState(outageLoad = cached, isLoading = cachedStreets == null)
            }
        }
    }

    /**
     * Startup sequence: 1 initial request + up to 2 retries with ~20s spread
     * if the server did not respond successfully. After that — no periodic polling;
     * refreshes happen only on explicit user actions or when network becomes available.
     */
    private fun startStartupRetrySequence() {
        startupJob?.cancel()
        startupJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 3
            while (isActive && attempts < maxAttempts) {
                attempts++
                val success = refreshFromNetwork(showLoading = attempts == 1)
                if (success) break
                if (attempts < maxAttempts) {
                    // Spread ~20s between retries (with small jitter)
                    delay(20_000L + (attempts * 2_000L))
                }
            }
        }
    }

    /**
     * Observe network connectivity. When network becomes available after being offline,
     * trigger a refresh so the UI updates dynamically without periodic polling.
     */
    private fun observeConnectivity() {
        connectivityJob?.cancel()
        connectivityJob = viewModelScope.launch {
            var wasOnline = isOnline
            ConnectivityMonitor.observe(appContext).collect { online ->
                if (online && !wasOnline) {
                    // Network just came back — refresh
                    refreshFromNetwork(showLoading = false)
                }
                wasOnline = online
            }
        }
    }

    /**
     * Returns true if the network request succeeded (API source).
     */
    private suspend fun refreshFromNetwork(showLoading: Boolean): Boolean {
        if (isRefreshing) return isOnline
        isRefreshing = true

        if (showLoading && latestOutageLoad == null) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        }

        var success = false
        runCatching {
            outageRepository.refreshCurrentOutages()
        }.onSuccess { fresh ->
            val apiSuccess = fresh.source == OutageSource.API
            if (apiSuccess) {
                isOnline = true
                success = true
                lastUpdatedMs = fresh.updatedAtMs ?: System.currentTimeMillis()
            } else {
                isOnline = false
            }
            latestOutageLoad = fresh
            renderState(outageLoad = fresh, isLoading = false)
        }.onFailure { error ->
            Log.w(TAG, "Network refresh failed", error)
            isOnline = false
            success = false
            if (latestOutageLoad == null) {
                runCatching { outageRepository.loadCurrentOutages() }
                    .onSuccess { fallback ->
                        success = fallback.source == OutageSource.API
                        latestOutageLoad = fallback
                        renderState(outageLoad = fallback, isLoading = false)
                    }
                    .onFailure { fallbackError ->
                        Log.e(TAG, "Fallback to local cache also failed", fallbackError)
                        renderState(
                            outageLoad = null,
                            isLoading = false,
                            errorMessage = "Нет соединения с сервером",
                        )
                    }
            } else {
                renderState(outageLoad = latestOutageLoad, isLoading = false)
            }
        }

        isRefreshing = false
        return success
    }

    private fun renderState(
        outageLoad: OutageLoadResult?,
        isLoading: Boolean,
        errorMessage: String? = null,
    ) {
        val streets = cachedStreets.orEmpty()
        val outages = outageLoad?.outages.orEmpty()
        val highlightByStreetNorm = buildHighlightMap(
            outages = outages,
            knownStreetNorms = streets.mapTo(linkedSetOf()) { it.streetNorm },
        )

        // Determine error message: explicit error, or offline with no data
        val effectiveError = when {
            errorMessage != null -> errorMessage
            !isOnline && outages.isEmpty() -> "Нет соединения с сервером"
            else -> null
        }

        _uiState.value = MapUiState(
            isLoading = isLoading,
            streets = streets,
            outages = outages,
            highlightByStreetNorm = highlightByStreetNorm,
            selectedStreet = _uiState.value.selectedStreet?.streetNorm?.let { streetNorm ->
                buildSelectedStreet(
                    streetNorm = streetNorm,
                    streets = streets,
                    outages = outages,
                    highlightByStreetNorm = highlightByStreetNorm,
                )
            },
            errorMessage = effectiveError,
            isOnline = isOnline,
            lastUpdatedLabel = lastUpdatedMs?.toHHmm()?.let { "Обновлено в $it" },
        )
    }

    private fun buildHighlightMap(
        outages: List<MapOutage>,
        knownStreetNorms: Set<String>,
    ): Map<String, OutageMapStatus> {
        val map = linkedMapOf<String, OutageMapStatus>()
        outages.forEach { outage ->
            outage.streetNorms.filter(knownStreetNorms::contains).forEach { norm ->
                val current = map[norm]
                map[norm] = when {
                    current == OutageMapStatus.ACTIVE -> OutageMapStatus.ACTIVE
                    outage.status == OutageMapStatus.ACTIVE -> OutageMapStatus.ACTIVE
                    else -> OutageMapStatus.PLANNED
                }
            }
        }
        return map
    }

    private fun buildSelectedStreet(
        streetNorm: String,
        streets: List<StreetFeature>,
        outages: List<MapOutage>,
        highlightByStreetNorm: Map<String, OutageMapStatus>,
    ): SelectedStreetUi? {
        val status = highlightByStreetNorm[streetNorm] ?: return null
        val street = streets.firstOrNull { it.streetNorm == streetNorm } ?: return null
        val streetOutages = outages.filter { streetNorm in it.streetNorms }
        if (streetOutages.isEmpty()) return null
        return SelectedStreetUi(
            streetNorm = streetNorm,
            streetName = street.displayName,
            status = status,
            outages = streetOutages,
        )
    }

    override fun onCleared() {
        super.onCleared()
        startupJob?.cancel()
        connectivityJob?.cancel()
    }
}

private const val TAG = "MapViewModel"

private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())

private fun Long.toHHmm(): String = HH_MM.format(Instant.ofEpochMilli(this))
