package ru.svetok.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.svetok.app.data.outage.MapOutage
import ru.svetok.app.data.outage.OutageMapStatus
import ru.svetok.app.data.outage.OutageType

@Composable
fun MapScreen(
    onReportStreet: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenOutagesList: () -> Unit = {},
    viewModel: MapViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                    )
                )
            )
    ) {
        OsmMapView(
            streets = uiState.streets,
            highlightByStreetNorm = uiState.highlightByStreetNorm,
            selectedStreetNorm = uiState.selectedStreet?.streetNorm,
            onStreetTap = viewModel::onStreetTapped,
            modifier = Modifier.fillMaxSize(),
        )

        StatusPanel(
            isLoading = uiState.isLoading,
            activeOutageCount = uiState.outages.count { it.status == OutageMapStatus.ACTIVE },
            plannedOutageCount = uiState.outages.count { it.status == OutageMapStatus.PLANNED },
            isOnline = uiState.isOnline,
            lastUpdatedLabel = uiState.lastUpdatedLabel,
            errorMessage = uiState.errorMessage,
            onRefresh = viewModel::refreshNow,
            onOpenSettings = onOpenSettings,
            onOpenOutagesList = onOpenOutagesList,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 18.dp),
        )

        uiState.selectedStreet?.let { selectedStreet ->
            StreetDetailsSheet(
                selectedStreet = selectedStreet,
                onDismiss = viewModel::clearStreetSelection,
                onReportStreet = { onReportStreet(selectedStreet.streetName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 18.dp),
            )
        }
    }
}

@Composable
private fun StatusPanel(
    isLoading: Boolean,
    activeOutageCount: Int,
    plannedOutageCount: Int,
    isOnline: Boolean,
    lastUpdatedLabel: String?,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenOutagesList: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.widthIn(max = 360.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ── Title row ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    GlitchTitleText(
                        text = "СветОк",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Карта отключений Светлограда",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, "Обновить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Notifications, "Уведомления",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenOutagesList) {
                        Icon(Icons.Default.List, "Активные отключения",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Status content ────────────────────────────────
            when {
                isLoading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        Text("Загрузка...", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                errorMessage != null -> {
                    ConnectionDot(isOnline = false)
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }

                else -> {
                    // Connection status
                    ConnectionDot(isOnline = isOnline, lastUpdatedLabel = lastUpdatedLabel)

                    // Outage counts or "all clear"
                    if (!isOnline && activeOutageCount == 0 && plannedOutageCount == 0) {
                        Text(
                            text = "Нет соединения с сервером",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "Проверьте интернет-соединение",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (activeOutageCount == 0 && plannedOutageCount == 0) {
                        NoOutagesRow()
                    } else {
                        LegendRow(activeOutageCount, plannedOutageCount)
                        Text(
                            text = "Нажмите на подсвеченную улицу для деталей.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionDot(isOnline: Boolean, lastUpdatedLabel: String? = null) {
    val green = Color(0xFF2E7D32)
    val dotColor = if (isOnline) green else MaterialTheme.colorScheme.error
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = if (isOnline) "Онлайн" else " ",
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
        )
        if (isOnline && lastUpdatedLabel != null) {
            Text(
                text = "· $lastUpdatedLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoOutagesRow() {
    val green = Color(0xFF2E7D32)
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓", style = MaterialTheme.typography.labelMedium, color = green)
        Text("Отключений нет", style = MaterialTheme.typography.labelMedium, color = green)
    }
}

@Composable
private fun LegendRow(activeOutageCount: Int, plannedOutageCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (activeOutageCount > 0)
            LegendItem(color = Color(0xFFC53A2D), label = "Активно: $activeOutageCount")
        if (plannedOutageCount > 0)
            LegendItem(color = Color(0xFFD47A00), label = "План: $plannedOutageCount")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color = color, shape = MaterialTheme.shapes.extraSmall),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StreetDetailsSheet(
    selectedStreet: SelectedStreetUi,
    onDismiss: () -> Unit,
    onReportStreet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.widthIn(max = 420.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = selectedStreet.streetName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selectedStreet.statusLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = selectedStreet.statusColor(),
                    )
                }
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }

            selectedStreet.outages.forEach { outage ->
                val typeLabel = outage.outageType.label
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = outage.containerColor().copy(alpha = 0.92f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = outage.timeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f),
                        )
                        if (!outage.reason.isNullOrBlank() && outage.reason != "Причина не указана") {
                            Text(
                                text = outage.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                            )
                        }
                    }
                }
            }

            Button(onClick = onReportStreet, modifier = Modifier.fillMaxWidth()) {
                Text("Сообщить о проблеме")
            }
        }
    }
}

private fun MapOutage.containerColor(): Color =
    when (outageType) {
        OutageType.EMERGENCY -> Color(0xFFC53A2D)
        OutageType.PLANNED -> Color(0xFFD47A00)
        OutageType.UNKNOWN -> Color(0xFF757575)
    }

private fun SelectedStreetUi.statusColor(): Color =
    when (status) {
        OutageMapStatus.ACTIVE -> Color(0xFFC53A2D)
        OutageMapStatus.PLANNED -> Color(0xFFD47A00)
    }

private fun SelectedStreetUi.statusLabel(): String =
    when (status) {
        OutageMapStatus.ACTIVE -> "Активно"
        OutageMapStatus.PLANNED -> "Запланировано"
    }
