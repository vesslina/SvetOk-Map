package ru.svetok.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.svetok.app.data.geo.GeoJsonRepository
import ru.svetok.app.data.subscription.HttpSubscriptionRepository
import ru.svetok.app.data.subscription.SubscriptionPrefs

data class StreetInfo(val norm: String, val displayName: String)

data class SettingsUiState(
    val fcmToken: String? = null,
    val searchQuery: String = "",
    val filteredStreets: List<StreetInfo> = emptyList(),
    val subscribedNorms: Set<String> = emptySet(),
    val isBusy: Boolean = false,
    val saveMessage: String? = null,
    val streetsLoadError: String? = null,
)

class SettingsViewModel(
    private val subscriptionRepository: HttpSubscriptionRepository,
    private val subscriptionPrefs: SubscriptionPrefs,
    private val fcmTokenProvider: FcmTokenProvider,
    private val geoJsonRepository: GeoJsonRepository,
) : ViewModel() {

    private var allStreets: List<StreetInfo> = emptyList()

    private val _uiState = MutableStateFlow(
        SettingsUiState(subscribedNorms = subscriptionPrefs.subscribedStreetNorms)
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { _uiState.update { it.copy(fcmToken = fcmTokenProvider.getToken()) } }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                geoJsonRepository.loadStreetFeatures()
                    .map { StreetInfo(it.streetNorm, it.displayName) }
                    .sortedBy { it.displayName }
            }.onSuccess { streets ->
                allStreets = streets
            }.onFailure { error ->
                Log.e(TAG, "Failed to load streets from geojson", error)
                _uiState.update { it.copy(streetsLoadError = "Не удалось загрузить список улиц") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val filtered = if (query.isBlank()) emptyList()
        else {
            val lower = query.trim().lowercase()
            allStreets.filter {
                it.displayName.lowercase().contains(lower) || it.norm.contains(lower)
            }.take(20)
        }
        _uiState.update { it.copy(searchQuery = query, filteredStreets = filtered) }
    }

    fun subscribe(street: StreetInfo) {
        val token = _uiState.value.fcmToken ?: return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val ok = runCatching {
                subscriptionRepository.upsertSubscription(fcmToken = token, streetNorm = street.norm)
            }.getOrElse { error ->
                Log.e(TAG, "Failed to subscribe to ${street.norm}", error)
                false
            }
            if (ok) subscriptionPrefs.addStreet(street.norm)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    subscribedNorms = subscriptionPrefs.subscribedStreetNorms,
                    saveMessage = if (ok) "Подписка на «${street.displayName}» сохранена." else "Ошибка соединения.",
                )
            }
        }
    }

    fun unsubscribe(street: StreetInfo) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            runCatching {
                subscriptionRepository.deleteSubscription(streetNorm = street.norm)
            }.onFailure { error ->
                Log.e(TAG, "Failed to unsubscribe from ${street.norm}", error)
            }
            subscriptionPrefs.removeStreet(street.norm)
            _uiState.update {
                it.copy(isBusy = false, subscribedNorms = subscriptionPrefs.subscribedStreetNorms)
            }
        }
    }

    fun clearSaveMessage() { _uiState.update { it.copy(saveMessage = null) } }
    fun clearStreetsLoadError() { _uiState.update { it.copy(streetsLoadError = null) } }
}

private const val TAG = "SettingsViewModel"
