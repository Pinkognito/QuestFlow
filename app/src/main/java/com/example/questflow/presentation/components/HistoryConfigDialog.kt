package com.example.questflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.questflow.data.preferences.HistoryEventConfig
import com.example.questflow.domain.model.HistoryCategory
import com.example.questflow.domain.model.HistoryEventType
import com.example.questflow.domain.model.HistoryPriority
import kotlin.math.roundToInt

/**
 * History Configuration Dialog
 *
 * Allows users to enable/disable individual history event types to control storage usage.
 * Organized by categories with priority indicators and dependency information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryConfigDialog(
    currentConfig: HistoryEventConfig,
    onDismiss: () -> Unit,
    onConfigChange: (HistoryEventConfig) -> Unit
) {
    var localConfig by remember { mutableStateOf(currentConfig) }
    var selectedCategory by remember { mutableStateOf<HistoryCategory?>(null) }

    Dialog(
        onDismissRequest = {
            onConfigChange(localConfig)
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                TopAppBar(
                    title = { Text("History-Konfiguration") },
                    navigationIcon = {
                        IconButton(onClick = {
                            onConfigChange(localConfig)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen")
                        }
                    },
                    actions = {
                        // Reset to defaults
                        IconButton(onClick = {
                            localConfig = HistoryEventConfig(HistoryEventConfig.getDefaultConfig())
                            onConfigChange(localConfig)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Zurücksetzen")
                        }
                    }
                )

                // Storage Impact Card
                StorageImpactCard(
                    config = localConfig,
                    modifier = Modifier.padding(16.dp)
                )

                // Quick Actions
                QuickActionsRow(
                    localConfig = localConfig,
                    onToggleAll = { enabled ->
                        val updatedEvents = localConfig.enabledEvents.mapValues { enabled }
                        localConfig = HistoryEventConfig(updatedEvents)
                        onConfigChange(localConfig)
                    },
                    onTogglePriority = { priority, enabled ->
                        val updatedEvents = localConfig.enabledEvents.toMutableMap()
                        HistoryEventType.values().filter { it.priority == priority }.forEach {
                            updatedEvents[it] = enabled
                        }
                        localConfig = HistoryEventConfig(updatedEvents)
                        onConfigChange(localConfig)
                    }
                )

                Divider()

                // Categories List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HistoryCategory.values().forEach { category ->
                        item {
                            CategorySection(
                                category = category,
                                config = localConfig,
                                isExpanded = selectedCategory == category,
                                onCategoryClick = {
                                    selectedCategory = if (selectedCategory == category) null else category
                                },
                                onToggleCategory = { enabled ->
                                    val updatedEvents = localConfig.enabledEvents.toMutableMap()
                                    HistoryEventType.getByCategory(category).forEach { eventType ->
                                        updatedEvents[eventType] = enabled
                                    }
                                    localConfig = HistoryEventConfig(updatedEvents)
                                    onConfigChange(localConfig)
                                },
                                onToggleEvent = { eventType, enabled ->
                                    val updatedEvents = localConfig.enabledEvents.toMutableMap()
                                    updatedEvents[eventType] = enabled
                                    localConfig = HistoryEventConfig(updatedEvents)
                                    onConfigChange(localConfig)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Storage Impact Card showing estimated storage usage
 */
@Composable
private fun StorageImpactCard(
    config: HistoryEventConfig,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Speicherverbrauch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "${config.enabledCount()}/${HistoryEventType.values().size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Täglich (ca.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${config.estimatedDailyStorageBytes()} Bytes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Monatlich (ca.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${config.estimatedMonthlyStorageKB().roundToInt()} KB",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Quick action buttons for bulk operations
 */
@Composable
private fun QuickActionsRow(
    localConfig: HistoryEventConfig,
    onToggleAll: (Boolean) -> Unit,
    onTogglePriority: (HistoryPriority, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Schnellaktionen",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Toggle All
            FilterChip(
                selected = false,
                onClick = { onToggleAll(true) },
                label = { Text("Alle an") },
                leadingIcon = { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = false,
                onClick = { onToggleAll(false) },
                label = { Text("Alle aus") },
                leadingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f)
            )
        }

        // Priority toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryPriority.values().forEach { priority ->
                val color = when (priority) {
                    HistoryPriority.CRITICAL -> Color(0xFFFF0000)
                    HistoryPriority.HIGH -> Color(0xFFFF8800)
                    HistoryPriority.MEDIUM -> Color(0xFFFFAA00)
                    HistoryPriority.LOW -> Color(0xFF00AA00)
                }

                FilterChip(
                    selected = HistoryEventType.values()
                        .filter { it.priority == priority }
                        .all { localConfig.enabledEvents[it] == true },
                    onClick = {
                        val allEnabled = HistoryEventType.values()
                            .filter { it.priority == priority }
                            .all { localConfig.enabledEvents[it] == true }
                        onTogglePriority(priority, !allEnabled)
                    },
                    label = { Text(priority.displayName, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Category Section with events
 */
@Composable
private fun CategorySection(
    category: HistoryCategory,
    config: HistoryEventConfig,
    isExpanded: Boolean,
    onCategoryClick: () -> Unit,
    onToggleCategory: (Boolean) -> Unit,
    onToggleEvent: (HistoryEventType, Boolean) -> Unit
) {
    val events = HistoryEventType.getByCategory(category)
    val allEnabled = events.all { config.enabledEvents[it] == true }
    val someEnabled = events.any { config.enabledEvents[it] == true }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Category Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = category.icon,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Column {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${events.count { config.enabledEvents[it] == true }}/${events.size} aktiv",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category toggle switch
                    Switch(
                        checked = allEnabled,
                        onCheckedChange = { onToggleCategory(it) }
                    )
                    IconButton(onClick = onCategoryClick) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Zuklappen" else "Aufklappen"
                        )
                    }
                }
            }

            // Expanded Event List
            if (isExpanded) {
                Divider()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    events.forEach { eventType ->
                        EventRow(
                            eventType = eventType,
                            isEnabled = config.enabledEvents[eventType] ?: false,
                            onToggle = { onToggleEvent(eventType, it) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Event Row
 */
@Composable
private fun EventRow(
    eventType: HistoryEventType,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val priorityColor = when (eventType.priority) {
        HistoryPriority.CRITICAL -> Color(0xFFFF0000)
        HistoryPriority.HIGH -> Color(0xFFFF8800)
        HistoryPriority.MEDIUM -> Color(0xFFFFAA00)
        HistoryPriority.LOW -> Color(0xFF00AA00)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = eventType.icon,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = eventType.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (eventType.priority == HistoryPriority.CRITICAL) FontWeight.Bold else FontWeight.Normal
                )
                // Priority indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(priorityColor, CircleShape)
                )
            }

            Text(
                text = eventType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp)
            )

            // Dependencies indicator
            if (eventType.dependencies.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = 32.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Hängt ab von: ${eventType.dependencies.joinToString { it.displayName }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Storage info
            Text(
                text = "${eventType.estimatedBytesPerEvent} Bytes/Event",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp, top = 2.dp)
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            enabled = eventType.priority != HistoryPriority.CRITICAL // Critical events should always be enabled
        )
    }
}
