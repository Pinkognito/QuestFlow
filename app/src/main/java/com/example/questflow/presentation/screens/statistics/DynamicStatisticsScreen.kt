package com.example.questflow.presentation.screens.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.questflow.domain.model.ChartTemplates
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.components.XpLevelBadge
import com.example.questflow.presentation.screens.statistics.chartbuilder.ChartConfigDialog
import com.example.questflow.presentation.screens.statistics.chartbuilder.DynamicChartCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicStatisticsScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: DynamicStatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val charts by viewModel.charts.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()

    // Sync category with viewmodel
    LaunchedEffect(selectedCategory) {
        viewModel.updateSelectedCategory(selectedCategory?.id)
    }

    // Auto-refresh charts when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshAllCharts()
    }

    Scaffold(
        topBar = {
            com.example.questflow.presentation.components.QuestFlowTopBar(
                title = "Statistiken",
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = appViewModel::selectCategory,
                onManageCategoriesClick = {
                    navController.navigate("categories")
                },
                level = globalStats?.level ?: 1,
                totalXp = globalStats?.xp ?: 0,
                actions = {
                    IconButton(onClick = { viewModel.toggleEditMode() }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = if (uiState.isEditMode) "Fertig" else "Bearbeiten",
                            tint = if (uiState.isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(visible = uiState.isEditMode) {
                    FloatingActionButton(
                        onClick = { viewModel.showTemplateDialog() },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aus Vorlage")
                    }
                }
                AnimatedVisibility(visible = uiState.isEditMode) {
                    FloatingActionButton(
                        onClick = { viewModel.showChartBuilder() }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Neues Chart")
                    }
                }
            }
        }
    ) { paddingValues ->
        if (charts.isEmpty()) {
            EmptyChartsPlaceholder(
                onAddFromTemplate = { viewModel.showTemplateDialog() },
                onCreateCustom = { viewModel.showChartBuilder() },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(charts, key = { it.id }) { chart ->
                    val data = chartData[chart.id]
                    if (data != null) {
                        DynamicChartCard(
                            config = chart,
                            data = data,
                            isEditMode = uiState.isEditMode,
                            onEditClick = { viewModel.showChartBuilder(existingChart = chart) },
                            onDeleteClick = { viewModel.deleteChart(chart) }
                        )
                    }
                }

                // Help text in edit mode
                if (uiState.isEditMode && charts.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "💡 Tipp: Mit ⚙️ bearbeitest du ein Chart, mit 🗑️ löschst du es. Der + Button fügt neue Charts hinzu.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Template Selection Dialog
        if (uiState.showTemplateDialog) {
            TemplateSelectionDialog(
                onDismiss = { viewModel.hideTemplateDialog() },
                onTemplateSelected = { template ->
                    viewModel.addChart(template.config)
                    viewModel.hideTemplateDialog()
                }
            )
        }

        // Chart Config Dialog
        if (uiState.showChartBuilder) {
            ChartConfigDialog(
                onDismiss = { viewModel.hideChartBuilder() },
                onSave = { config ->
                    if (uiState.editingChart != null) {
                        viewModel.updateChart(config.copy(id = uiState.editingChart!!.id))
                    } else {
                        viewModel.addChart(config)
                    }
                    viewModel.hideChartBuilder()
                },
                existingConfig = uiState.editingChart ?: uiState.selectedTemplate?.config
            )
        }
    }
}

@Composable
fun EmptyChartsPlaceholder(
    onAddFromTemplate: () -> Unit,
    onCreateCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Keine Charts vorhanden",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Erstelle deine eigenen Charts und Statistiken mit dem dynamischen Chart-Builder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onAddFromTemplate) {
                        Text("Aus Vorlage")
                    }
                    OutlinedButton(onClick = onCreateCustom) {
                        Text("Eigenes Chart")
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateSelectionDialog(
    onDismiss: () -> Unit,
    onTemplateSelected: (com.example.questflow.domain.model.ChartTemplate) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Vorlage auswählen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(ChartTemplates.templates) { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onTemplateSelected(template) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = template.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = template.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
