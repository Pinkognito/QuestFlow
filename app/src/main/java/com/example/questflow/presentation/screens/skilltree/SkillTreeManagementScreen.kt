package com.example.questflow.presentation.screens.skilltree

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.components.QuestFlowTopBar
import com.example.questflow.presentation.components.SkillTreeCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillTreeManagementScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: SkillTreeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val edges by viewModel.edges.collectAsState()
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()

    var selectedSkillId by remember { mutableStateOf<String?>(null) }
    var showSkillCreator by remember { mutableStateOf(false) }
    var showSkillLinker by remember { mutableStateOf(false) }
    var showSkillDetails by remember { mutableStateOf(false) }
    var showSkillEditor by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Sync category with viewmodel
    LaunchedEffect(selectedCategory) {
        viewModel.updateSelectedCategory(selectedCategory?.id)
    }

    // Show notification snackbar
    LaunchedEffect(uiState.notification) {
        uiState.notification?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        topBar = {
            Column {
                QuestFlowTopBar(
                    title = "Skill Tree",
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onCategorySelected = appViewModel::selectCategory,
                    onManageCategoriesClick = { navController.navigate("categories") },
                    level = globalStats?.level ?: 1,
                    totalXp = globalStats?.xp ?: 0
                )
                // Skillpoints display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Verfügbare Skillpunkte",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${uiState.availablePoints} SP",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!editMode) {
                            Text(
                                text = "Wähle einen Skill zum Investieren",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle Edit Mode
                SmallFloatingActionButton(
                    onClick = { editMode = !editMode },
                    containerColor = if (editMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (editMode) Icons.Default.Done else Icons.Default.Edit,
                        contentDescription = if (editMode) "Bearbeitung beenden" else "Bearbeiten"
                    )
                }

                // Add new skill
                FloatingActionButton(
                    onClick = { showSkillCreator = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Skill hinzufügen")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.skills.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Noch keine Skills",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (selectedCategory != null)
                            "Erstelle deinen ersten Skill für diese Kategorie!"
                        else
                            "Erstelle globale Skills oder wähle eine Kategorie aus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showSkillCreator = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Skill erstellen")
                    }
                }
            } else {
                // Skill Tree Canvas
                SkillTreeCanvas(
                    skills = uiState.skills,
                    edges = edges.filter { edge ->
                        // Only show edges where both nodes exist in current category
                        uiState.skills.any { it.node.id == edge.parentId } &&
                        uiState.skills.any { it.node.id == edge.childId }
                    },
                    selectedSkillId = selectedSkillId,
                    editMode = editMode,
                    onSkillClick = { skillId ->
                        selectedSkillId = if (selectedSkillId == skillId) null else skillId
                        if (!editMode) {
                            showSkillDetails = true
                        }
                    },
                    onSkillPositionChange = { nodeId, x, y ->
                        viewModel.updateSkillPosition(nodeId, x, y)
                    }
                )
            }

            // Skill Details Bottom Sheet (when selected in non-edit mode)
            if (selectedSkillId != null && showSkillDetails && !editMode) {
                val selectedSkill = uiState.skills.find { it.node.id == selectedSkillId }
                selectedSkill?.let { skill ->
                    SkillDetailsBottomSheet(
                        skill = skill,
                        availablePoints = uiState.availablePoints,
                        onDismiss = {
                            selectedSkillId = null
                            showSkillDetails = false
                        },
                        onInvest = {
                            viewModel.investSkillPoint(skill.node.id)
                        },
                        onRefund = {
                            viewModel.refundSkillPoint(skill.node.id)
                        }
                    )
                }
            }

            // Edit Mode Actions Bottom Sheet
            if (selectedSkillId != null && editMode) {
                val selectedSkill = uiState.skills.find { it.node.id == selectedSkillId }
                selectedSkill?.let { skill ->
                    SkillEditActionsBottomSheet(
                        skill = skill,
                        onDismiss = {
                            selectedSkillId = null
                        },
                        onEdit = {
                            showSkillEditor = true
                        },
                        onAddConnection = {
                            showSkillLinker = true
                        },
                        onDelete = {
                            viewModel.deleteSkill(skill.node.id)
                            selectedSkillId = null
                        }
                    )
                }
            }
        }
    }

    // Skill Creator Dialog
    if (showSkillCreator) {
        SkillCreatorDialog(
            availableSkills = uiState.skills,
            onDismiss = { showSkillCreator = false },
            onCreateSkill = { title, desc, effect, base, scaling, maxInv, color, parentSkills ->
                viewModel.createSkillWithParents(title, desc, effect, base, scaling, maxInv, color, parentSkills)
                showSkillCreator = false
            }
        )
    }

    // Skill Editor Dialog
    if (showSkillEditor && selectedSkillId != null) {
        val selectedSkill = uiState.skills.find { it.node.id == selectedSkillId }
        selectedSkill?.let { skill ->
            SkillEditorDialog(
                skill = skill,
                onDismiss = { showSkillEditor = false },
                onSave = { title, desc, base, scaling, maxInv, color ->
                    viewModel.updateSkill(skill.node.id, title, desc, base, scaling, maxInv, color)
                    showSkillEditor = false
                }
            )
        }
    }

    // Skill Linker Dialog
    if (showSkillLinker && selectedSkillId != null) {
        val selectedSkill = uiState.skills.find { it.node.id == selectedSkillId }
        selectedSkill?.let { skill ->
            val existingParents = edges.filter { it.childId == skill.node.id }.map { it.parentId }

            SkillLinkDialog(
                currentSkill = skill,
                availableSkills = uiState.skills,
                existingParents = existingParents,
                onDismiss = { showSkillLinker = false },
                onLinkSkill = { parentId, minInv ->
                    viewModel.linkSkill(skill.node.id, parentId, minInv)
                    showSkillLinker = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillDetailsBottomSheet(
    skill: com.example.questflow.data.repository.SkillNodeWithStatus,
    availablePoints: Int,
    onDismiss: () -> Unit,
    onInvest: () -> Unit,
    onRefund: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = skill.node.title,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = skill.node.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Investment progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Investment:")
                Text(
                    text = "${skill.currentInvestment} / ${skill.node.maxInvestment}",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }

            if (skill.node.maxInvestment > 1) {
                LinearProgressIndicator(
                    progress = { skill.currentInvestment.toFloat() / skill.node.maxInvestment },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (skill.currentInvestment > 0) {
                    OutlinedButton(
                        onClick = onRefund,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Refund")
                    }
                }

                val isMaxed = skill.currentInvestment >= skill.node.maxInvestment
                if (!isMaxed) {
                    Button(
                        onClick = onInvest,
                        enabled = skill.isAvailable && availablePoints >= 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Invest (1 SP)")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillEditActionsBottomSheet(
    skill: com.example.questflow.data.repository.SkillNodeWithStatus,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAddConnection: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Skill bearbeiten",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = skill.node.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Edit skill
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Skill bearbeiten")
            }

            Spacer(Modifier.height(8.dp))

            // Add connection
            OutlinedButton(
                onClick = onAddConnection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Voraussetzung hinzufügen")
            }

            Spacer(Modifier.height(8.dp))

            // Delete skill
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Skill löschen")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
