package com.example.questflow.presentation.screens.skilltree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.data.database.entity.SkillType
import com.example.questflow.presentation.components.XpLevelBadge
import com.example.questflow.presentation.components.QuestFlowTopBar
import com.example.questflow.presentation.AppViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillTreeScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    viewModel: SkillTreeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()
    var selectedNodeId by remember { mutableStateOf<String?>(null) }

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
            QuestFlowTopBar(
                title = "Skill Tree",
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = appViewModel::selectCategory,
                onManageCategoriesClick = { navController.navigate("categories") },
                level = globalStats?.level ?: 1,
                totalXp = globalStats?.xp ?: 0
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Skill Points Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Available Skill Points",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${uiState.availablePoints}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Skill Nodes
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.skills) { skillNode ->
                    SkillNodeCard(
                        skillNode = skillNode,
                        isSelected = selectedNodeId == skillNode.node.id,
                        canAfford = uiState.availablePoints >= 1,
                        onNodeClick = {
                            selectedNodeId = if (selectedNodeId == skillNode.node.id) null else skillNode.node.id
                        },
                        onInvest = {
                            viewModel.investSkillPoint(skillNode.node.id)
                        },
                        onRefund = {
                            viewModel.refundSkillPoint(skillNode.node.id)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillNodeCard(
    skillNode: com.example.questflow.data.repository.SkillNodeWithStatus,
    isSelected: Boolean,
    canAfford: Boolean,
    onNodeClick: () -> Unit,
    onInvest: () -> Unit,
    onRefund: () -> Unit
) {
    val icon = when (skillNode.node.effectType) {
        com.example.questflow.data.database.entity.SkillEffectType.XP_MULTIPLIER -> Icons.Default.Star
        com.example.questflow.data.database.entity.SkillEffectType.STREAK_PROTECTION -> Icons.Default.Lock
        com.example.questflow.data.database.entity.SkillEffectType.EXTRA_COLLECTION_UNLOCK -> Icons.Default.Favorite
        com.example.questflow.data.database.entity.SkillEffectType.SKILL_POINT_GAIN -> Icons.Default.Add
        else -> Icons.Default.Star
    }

    val isMaxed = skillNode.currentInvestment >= skillNode.node.maxInvestment
    val canInvest = skillNode.isAvailable && canAfford && !isMaxed

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onNodeClick,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isMaxed -> MaterialTheme.colorScheme.primaryContainer
                skillNode.isUnlocked -> MaterialTheme.colorScheme.secondaryContainer
                skillNode.isAvailable -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = when {
                        isMaxed -> MaterialTheme.colorScheme.primary
                        skillNode.isUnlocked -> MaterialTheme.colorScheme.secondary
                        skillNode.isAvailable -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = skillNode.node.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${skillNode.currentInvestment}/${skillNode.node.maxInvestment}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isMaxed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = skillNode.node.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Show effect calculation
                    val currentEffect = skillNode.node.baseValue + (skillNode.node.scalingPerPoint * skillNode.currentInvestment)
                    val nextEffect = skillNode.node.baseValue + (skillNode.node.scalingPerPoint * (skillNode.currentInvestment + 1))

                    if (skillNode.currentInvestment > 0) {
                        Text(
                            text = "Aktueller Effekt: +${currentEffect.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!isMaxed && skillNode.isAvailable) {
                        Text(
                            text = "Nächster Level: +${nextEffect.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (!skillNode.isAvailable && skillNode.prerequisitesInfo.isNotEmpty()) {
                        Text(
                            text = "Benötigt: ${skillNode.prerequisitesInfo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (isMaxed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Maxed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else if (!skillNode.isAvailable) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (isSelected && skillNode.isAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (skillNode.currentInvestment > 0) {
                        OutlinedButton(
                            onClick = onRefund,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Refund")
                        }
                    }

                    if (!isMaxed) {
                        Button(
                            onClick = onInvest,
                            enabled = canInvest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Invest (1 SP)")
                        }
                    }
                }
            }
        }
    }
}