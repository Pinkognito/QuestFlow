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

    // Sync category with viewmodel
    LaunchedEffect(selectedCategory) {
        viewModel.updateSelectedCategory(selectedCategory?.id)
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
        }
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
                        onNodeClick = { selectedNodeId = skillNode.node.id },
                        onUnlock = {
                            viewModel.unlockSkill(skillNode.node.id)
                            selectedNodeId = null
                        }
                    )
                }
            }
        }
    }

    // Show notification
    uiState.notification?.let { message ->
        // In production, you'd show a Snackbar here
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillNodeCard(
    skillNode: com.example.questflow.data.repository.SkillNodeWithStatus,
    isSelected: Boolean,
    canAfford: Boolean,
    onNodeClick: () -> Unit,
    onUnlock: () -> Unit
) {
    val icon = when (skillNode.node.type) {
        SkillType.XP_MULT -> Icons.Default.Star
        SkillType.STREAK_GUARD -> Icons.Default.Lock
        SkillType.EXTRA_MEME -> Icons.Default.Favorite
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (!skillNode.isUnlocked && skillNode.isAvailable) onNodeClick else { {} },
        colors = CardDefaults.cardColors(
            containerColor = when {
                skillNode.isUnlocked -> MaterialTheme.colorScheme.primaryContainer
                skillNode.isAvailable -> MaterialTheme.colorScheme.secondaryContainer
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
                        skillNode.isUnlocked -> MaterialTheme.colorScheme.primary
                        skillNode.isAvailable -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = skillNode.node.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = skillNode.node.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!skillNode.isUnlocked) {
                        Text(
                            text = "Cost: 1 Skill Point",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (skillNode.isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Unlocked",
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

            if (isSelected && skillNode.isAvailable && !skillNode.isUnlocked) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onUnlock,
                    enabled = canAfford,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock Skill")
                }
            }
        }
    }
}