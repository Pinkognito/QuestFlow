package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.questflow.domain.model.*
import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.database.entity.TaskFilterPresetEntity

/**
 * Advanced Task Filter Dialog with tabs for:
 * - Filter (comprehensive filter options)
 * - Sort (multi-level sorting)
 * - Group (grouping options)
 * - Presets (save/load filter presets)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTaskFilterDialog(
    currentFilter: AdvancedTaskFilter,
    categories: List<CategoryEntity>,
    presets: List<TaskFilterPresetEntity>,
    onDismiss: () -> Unit,
    onApply: (AdvancedTaskFilter) -> Unit,
    onSavePreset: (AdvancedTaskFilter, String, String) -> Unit,
    onLoadPreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit
) {
    var localFilter by remember { mutableStateOf(currentFilter) }
    var selectedTab by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = {
            android.util.Log.d("FilterDialog", "onDismissRequest - Applying filter and closing...")
            onApply(localFilter)
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
                    title = { Text("Erweiterte Filter & Sortierung") },
                    navigationIcon = {
                        IconButton(onClick = {
                            android.util.Log.d("FilterDialog", "Close button - Applying filter and closing...")
                            onApply(localFilter)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen")
                        }
                    },
                    actions = {
                        // Reset button
                        IconButton(onClick = {
                            localFilter = AdvancedTaskFilter()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Zurücksetzen")
                        }
                    }
                )

                // Tab Row
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Filter") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Sortierung") },
                        icon = { Icon(Icons.Default.List, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Gruppierung") },
                        icon = { Icon(Icons.Default.AccountBox, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Presets") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )
                }

                // Tab Content
                when (selectedTab) {
                    0 -> FilterTab(
                        filter = localFilter,
                        categories = categories,
                        onFilterChange = { localFilter = it }
                    )
                    1 -> SortTab(
                        sortOptions = localFilter.sortOptions,
                        onSortOptionsChange = { localFilter = localFilter.copy(sortOptions = it) }
                    )
                    2 -> GroupTab(
                        groupBy = localFilter.groupBy,
                        onGroupByChange = { localFilter = localFilter.copy(groupBy = it) }
                    )
                    3 -> PresetsTab(
                        presets = presets,
                        currentFilter = localFilter,
                        onSavePreset = onSavePreset,
                        onLoadPreset = { presetId ->
                            // Load preset applies it directly in ViewModel
                            // Close dialog after loading
                            onLoadPreset(presetId)
                            onDismiss()
                        },
                        onDeletePreset = onDeletePreset
                    )
                }
            }
        }
    }
}

/**
 * Filter Tab - All filter options
 */
@Composable
private fun FilterTab(
    filter: AdvancedTaskFilter,
    categories: List<CategoryEntity>,
    onFilterChange: (AdvancedTaskFilter) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Filter Section
        item {
            FilterSection(
                title = "Status",
                enabled = filter.statusFilters.enabled,
                onEnabledChange = { enabled ->
                    onFilterChange(filter.copy(statusFilters = filter.statusFilters.copy(enabled = enabled)))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterCheckbox(
                        label = "Abgeschlossen",
                        checked = filter.statusFilters.showCompleted,
                        onCheckedChange = {
                            onFilterChange(filter.copy(statusFilters = filter.statusFilters.copy(showCompleted = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Offen",
                        checked = filter.statusFilters.showOpen,
                        onCheckedChange = {
                            onFilterChange(filter.copy(statusFilters = filter.statusFilters.copy(showOpen = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Abgelaufen",
                        checked = filter.statusFilters.showExpired,
                        onCheckedChange = {
                            onFilterChange(filter.copy(statusFilters = filter.statusFilters.copy(showExpired = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Claimed",
                        checked = filter.statusFilters.showClaimed,
                        onCheckedChange = {
                            onFilterChange(filter.copy(statusFilters = filter.statusFilters.copy(showClaimed = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Unclaimed",
                        checked = filter.statusFilters.showUnclaimed,
                        onCheckedChange = {
                            onFilterChange(filter.copy(statusFilters = filter.statusFilters.copy(showUnclaimed = it)))
                        }
                    )
                }
            }
        }

        // Priority Filter Section
        item {
            FilterSection(
                title = "Priorität",
                enabled = filter.priorityFilters.enabled,
                onEnabledChange = { enabled ->
                    onFilterChange(filter.copy(priorityFilters = filter.priorityFilters.copy(enabled = enabled)))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterCheckbox(
                        label = "Dringend (URGENT)",
                        checked = filter.priorityFilters.showUrgent,
                        onCheckedChange = {
                            onFilterChange(filter.copy(priorityFilters = filter.priorityFilters.copy(showUrgent = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Hoch (HIGH)",
                        checked = filter.priorityFilters.showHigh,
                        onCheckedChange = {
                            onFilterChange(filter.copy(priorityFilters = filter.priorityFilters.copy(showHigh = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Mittel (MEDIUM)",
                        checked = filter.priorityFilters.showMedium,
                        onCheckedChange = {
                            onFilterChange(filter.copy(priorityFilters = filter.priorityFilters.copy(showMedium = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Niedrig (LOW)",
                        checked = filter.priorityFilters.showLow,
                        onCheckedChange = {
                            onFilterChange(filter.copy(priorityFilters = filter.priorityFilters.copy(showLow = it)))
                        }
                    )
                }
            }
        }

        // Category Filter Section
        item {
            FilterSection(
                title = "Kategorien",
                enabled = filter.categoryFilters.enabled,
                onEnabledChange = { enabled ->
                    onFilterChange(filter.copy(categoryFilters = filter.categoryFilters.copy(enabled = enabled)))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { category ->
                        FilterCheckbox(
                            label = "${category.emoji} ${category.name}",
                            checked = filter.categoryFilters.selectedCategoryIds.contains(category.id),
                            onCheckedChange = { checked ->
                                val newSet = if (checked) {
                                    filter.categoryFilters.selectedCategoryIds + category.id
                                } else {
                                    filter.categoryFilters.selectedCategoryIds - category.id
                                }
                                onFilterChange(filter.copy(categoryFilters = filter.categoryFilters.copy(selectedCategoryIds = newSet)))
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    FilterCheckbox(
                        label = "Ohne Kategorie",
                        checked = filter.categoryFilters.includeUncategorized,
                        onCheckedChange = {
                            onFilterChange(filter.copy(categoryFilters = filter.categoryFilters.copy(includeUncategorized = it)))
                        }
                    )
                }
            }
        }

        // XP/Difficulty Filter Section
        item {
            FilterSection(
                title = "Schwierigkeitsgrad / XP",
                enabled = filter.xpFilters.enabled,
                onEnabledChange = { enabled ->
                    onFilterChange(filter.copy(xpFilters = filter.xpFilters.copy(enabled = enabled)))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterCheckbox(
                        label = "Trivial (20%)",
                        checked = filter.xpFilters.difficultyLevels.contains(20),
                        onCheckedChange = { checked ->
                            val newSet = if (checked) {
                                filter.xpFilters.difficultyLevels + 20
                            } else {
                                filter.xpFilters.difficultyLevels - 20
                            }
                            onFilterChange(filter.copy(xpFilters = filter.xpFilters.copy(difficultyLevels = newSet)))
                        }
                    )
                    FilterCheckbox(
                        label = "Einfach (40%)",
                        checked = filter.xpFilters.difficultyLevels.contains(40),
                        onCheckedChange = { checked ->
                            val newSet = if (checked) {
                                filter.xpFilters.difficultyLevels + 40
                            } else {
                                filter.xpFilters.difficultyLevels - 40
                            }
                            onFilterChange(filter.copy(xpFilters = filter.xpFilters.copy(difficultyLevels = newSet)))
                        }
                    )
                    FilterCheckbox(
                        label = "Mittel (60%)",
                        checked = filter.xpFilters.difficultyLevels.contains(60),
                        onCheckedChange = { checked ->
                            val newSet = if (checked) {
                                filter.xpFilters.difficultyLevels + 60
                            } else {
                                filter.xpFilters.difficultyLevels - 60
                            }
                            onFilterChange(filter.copy(xpFilters = filter.xpFilters.copy(difficultyLevels = newSet)))
                        }
                    )
                    FilterCheckbox(
                        label = "Schwer (80%)",
                        checked = filter.xpFilters.difficultyLevels.contains(80),
                        onCheckedChange = { checked ->
                            val newSet = if (checked) {
                                filter.xpFilters.difficultyLevels + 80
                            } else {
                                filter.xpFilters.difficultyLevels - 80
                            }
                            onFilterChange(filter.copy(xpFilters = filter.xpFilters.copy(difficultyLevels = newSet)))
                        }
                    )
                    FilterCheckbox(
                        label = "Episch (100%)",
                        checked = filter.xpFilters.difficultyLevels.contains(100),
                        onCheckedChange = { checked ->
                            val newSet = if (checked) {
                                filter.xpFilters.difficultyLevels + 100
                            } else {
                                filter.xpFilters.difficultyLevels - 100
                            }
                            onFilterChange(filter.copy(xpFilters = filter.xpFilters.copy(difficultyLevels = newSet)))
                        }
                    )
                }
            }
        }

        // Date Filter Section
        item {
            FilterSection(
                title = "Datum & Zeit",
                enabled = filter.dateFilters.enabled,
                onEnabledChange = { enabled ->
                    onFilterChange(filter.copy(dateFilters = filter.dateFilters.copy(enabled = enabled)))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Zeitbereich:", style = MaterialTheme.typography.labelMedium)
                    DateFilterType.entries.forEach { type ->
                        FilterChip(
                            selected = filter.dateFilters.filterType == type,
                            onClick = {
                                onFilterChange(filter.copy(dateFilters = filter.dateFilters.copy(filterType = type)))
                            },
                            label = {
                                Text(when (type) {
                                    DateFilterType.ALL -> "Alle"
                                    DateFilterType.TODAY -> "Heute"
                                    DateFilterType.YESTERDAY -> "Gestern"
                                    DateFilterType.THIS_WEEK -> "Diese Woche"
                                    DateFilterType.LAST_WEEK -> "Letzte Woche"
                                    DateFilterType.THIS_MONTH -> "Dieser Monat"
                                    DateFilterType.LAST_MONTH -> "Letzter Monat"
                                    DateFilterType.THIS_YEAR -> "Dieses Jahr"
                                    DateFilterType.NEXT_7_DAYS -> "Nächste 7 Tage"
                                    DateFilterType.NEXT_30_DAYS -> "Nächste 30 Tage"
                                    DateFilterType.CUSTOM_RANGE -> "Benutzerdefiniert"
                                })
                            }
                        )
                    }
                }
            }
        }

        // Metadata Filter Section
        item {
            FilterSection(
                title = "Metadaten",
                enabled = filter.metadataFilters.enabled,
                onEnabledChange = { enabled ->
                    onFilterChange(filter.copy(metadataFilters = filter.metadataFilters.copy(enabled = enabled)))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TriStateFilterCheckbox(
                        label = "Kontakte",
                        state = filter.metadataFilters.hasContacts,
                        onStateChange = {
                            onFilterChange(filter.copy(metadataFilters = filter.metadataFilters.copy(hasContacts = it)))
                        }
                    )
                    TriStateFilterCheckbox(
                        label = "Standorte",
                        state = filter.metadataFilters.hasLocations,
                        onStateChange = {
                            onFilterChange(filter.copy(metadataFilters = filter.metadataFilters.copy(hasLocations = it)))
                        }
                    )
                    TriStateFilterCheckbox(
                        label = "Notizen",
                        state = filter.metadataFilters.hasNotes,
                        onStateChange = {
                            onFilterChange(filter.copy(metadataFilters = filter.metadataFilters.copy(hasNotes = it)))
                        }
                    )
                    TriStateFilterCheckbox(
                        label = "Anhänge",
                        state = filter.metadataFilters.hasAttachments,
                        onStateChange = {
                            onFilterChange(filter.copy(metadataFilters = filter.metadataFilters.copy(hasAttachments = it)))
                        }
                    )
                    TriStateFilterCheckbox(
                        label = "Kalendereintrag",
                        state = filter.metadataFilters.hasCalendarEvent,
                        onStateChange = {
                            onFilterChange(filter.copy(metadataFilters = filter.metadataFilters.copy(hasCalendarEvent = it)))
                        }
                    )
                }
            }
        }

        // Recurring Filter Section
        item {
            FilterSection(
                title = "Wiederkehrend",
                enabled = filter.recurringFilters.enabled,
                onEnabledChange = { enabled ->
                    onFilterChange(filter.copy(recurringFilters = filter.recurringFilters.copy(enabled = enabled)))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterCheckbox(
                        label = "Wiederkehrende Tasks",
                        checked = filter.recurringFilters.showRecurring,
                        onCheckedChange = {
                            onFilterChange(filter.copy(recurringFilters = filter.recurringFilters.copy(showRecurring = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Einmalige Tasks",
                        checked = filter.recurringFilters.showNonRecurring,
                        onCheckedChange = {
                            onFilterChange(filter.copy(recurringFilters = filter.recurringFilters.copy(showNonRecurring = it)))
                        }
                    )
                }
            }
        }

        // Relationship Filter Section
        item {
            FilterSection(
                title = "Beziehungen (Parent/Subtask)",
                enabled = filter.relationshipFilters.enabled,
                onEnabledChange = { enabled ->
                    onFilterChange(filter.copy(relationshipFilters = filter.relationshipFilters.copy(enabled = enabled)))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterCheckbox(
                        label = "Übergeordnete Tasks",
                        checked = filter.relationshipFilters.showParentTasks,
                        onCheckedChange = {
                            onFilterChange(filter.copy(relationshipFilters = filter.relationshipFilters.copy(showParentTasks = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Subtasks",
                        checked = filter.relationshipFilters.showSubtasks,
                        onCheckedChange = {
                            onFilterChange(filter.copy(relationshipFilters = filter.relationshipFilters.copy(showSubtasks = it)))
                        }
                    )
                    FilterCheckbox(
                        label = "Eigenständige Tasks",
                        checked = filter.relationshipFilters.showStandalone,
                        onCheckedChange = {
                            onFilterChange(filter.copy(relationshipFilters = filter.relationshipFilters.copy(showStandalone = it)))
                        }
                    )
                }
            }
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Sort Tab - Multi-level sorting
 */
@Composable
private fun SortTab(
    sortOptions: List<SortOption>,
    onSortOptionsChange: (List<SortOption>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info text
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(
                "Sortierung wird in der angegebenen Reihenfolge angewendet. Die erste Sortierung hat die höchste Priorität.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Current sort options
        if (sortOptions.isNotEmpty()) {
            Text(
                "Aktive Sortierung:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            sortOptions.forEachIndexed { index, option ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(option.displayName)
                        }
                        IconButton(onClick = {
                            onSortOptionsChange(sortOptions.filterIndexed { i, _ -> i != index })
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Entfernen")
                        }
                    }
                }
            }
        }

        // Add sort button
        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sortierung hinzufügen")
        }
    }

    // Add sort dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Sortierung hinzufügen") },
            text = {
                LazyColumn {
                    items(SortOption.entries) { option ->
                        OutlinedCard(
                            onClick = {
                                onSortOptionsChange(sortOptions + option)
                                showAddDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                option.displayName,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Group Tab - Grouping options
 */
@Composable
private fun GroupTab(
    groupBy: GroupByOption,
    onGroupByChange: (GroupByOption) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Tasks gruppieren nach:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(GroupByOption.entries) { option ->
            FilterChip(
                selected = groupBy == option,
                onClick = { onGroupByChange(option) },
                label = { Text(option.displayName) },
                leadingIcon = if (groupBy == option) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Presets Tab - Save/Load presets
 */
@Composable
private fun PresetsTab(
    presets: List<TaskFilterPresetEntity>,
    currentFilter: AdvancedTaskFilter,
    onSavePreset: (AdvancedTaskFilter, String, String) -> Unit,
    onLoadPreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Save current filter button
        Button(
            onClick = { showSaveDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Aktuellen Filter speichern")
        }

        if (presets.isNotEmpty()) {
            Text(
                "Gespeicherte Presets:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { preset ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (preset.isDefault)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        preset.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (preset.isDefault) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge { Text("Standard") }
                                    }
                                }
                                if (preset.description.isNotBlank()) {
                                    Text(
                                        preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = { onLoadPreset(preset.id) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Laden")
                                }
                                IconButton(onClick = { onDeletePreset(preset.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save preset dialog
    if (showSaveDialog) {
        var presetName by remember { mutableStateOf("") }
        var presetDescription by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Preset speichern") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = presetDescription,
                        onValueChange = { presetDescription = it },
                        label = { Text("Beschreibung (optional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSavePreset(currentFilter, presetName, presetDescription)
                            showSaveDialog = false
                        }
                    },
                    enabled = presetName.isNotBlank()
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

// === HELPER COMPOSABLES ===

@Composable
private fun FilterSection(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun FilterCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TriStateFilterCheckbox(
    label: String,
    state: Boolean?,
    onStateChange: (Boolean?) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        TriStateCheckbox(
            state = when (state) {
                true -> androidx.compose.ui.state.ToggleableState.On
                false -> androidx.compose.ui.state.ToggleableState.Off
                null -> androidx.compose.ui.state.ToggleableState.Indeterminate
            },
            onClick = {
                onStateChange(when (state) {
                    null -> true
                    true -> false
                    false -> null
                })
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label)
            Text(
                when (state) {
                    null -> "Ignorieren"
                    true -> "Muss vorhanden sein"
                    false -> "Darf nicht vorhanden sein"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
