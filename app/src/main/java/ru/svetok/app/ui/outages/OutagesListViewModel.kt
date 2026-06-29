package ru.svetok.app.ui.outages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.svetok.app.data.outage.OutageMapStatus
import ru.svetok.app.data.outage.OutageRepository
import ru.svetok.app.data.outage.OutageSource
import ru.svetok.app.data.outage.OutageType

data class OutageListItem(
    val id: String,
    val intId: Int?,
    val displayTitle: String,
    val timeLabel: String,
    val reason: String?,
    val streets: List<String>,
    val status: OutageMapStatus,
    val outageType: OutageType,
)

data class OutagesListUiState(
    val isLoading: Boolean = false,
    val outages: List<OutageListItem> = emptyList(),
    val isOffline: Boolean = false,
)

class OutagesListViewModel(
    private val outageRepo: OutageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutagesListUiState())
    val uiState: StateFlow<OutagesListUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val result = outageRepo.refreshCurrentOutages()
                val isOffline = result.source != OutageSource.API
                val items = result.outages.map { o ->
                    OutageListItem(
                        id = o.id,
                        intId = o.id.toIntOrNull(),
                        displayTitle = o.outageType.label,
                        timeLabel = o.timeLabel,
                        reason = o.reason.takeUnless { it.isBlank() || it == "Причина не указана" },
                        streets = o.streetLabels,
                        status = o.status,
                        outageType = o.outageType,
                    )
                }
                _uiState.update {
                    it.copy(isLoading = false, outages = items, isOffline = isOffline)
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isOffline = true)
                }
            }
        }
    }
}
