package com.example.questflow.presentation.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.questflow.presentation.components.XpBurstAnimation
import com.example.questflow.presentation.components.XpLevelBadge
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarXpScreen(
    viewModel: CalendarXpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        XpLevelBadge(
                            level = uiState.level,
                            currentXp = uiState.totalXp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
        if (uiState.links.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No calendar events yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.links) { link ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    link.title,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Column {
                                    Text(
                                        "Starts: ${link.startsAt.format(dateFormatter)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    val difficultyText = when (link.xpPercentage) {
                                        20 -> "Trivial"
                                        40 -> "Einfach"
                                        60 -> "Mittel"
                                        80 -> "Schwer"
                                        100 -> "Episch"
                                        else -> "Mittel"
                                    }
                                    Text(
                                        "Schwierigkeit: $difficultyText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            trailingContent = {
                                Button(
                                    onClick = { viewModel.claimXp(link.id) },
                                    enabled = !link.rewarded
                                ) {
                                    Text(
                                        if (link.rewarded) "Claimed" else "Claim XP"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
        }

        // Show XP animation overlay
        uiState.xpAnimationData?.let { animationData ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                XpBurstAnimation(
                    xpAmount = animationData.xpAmount,
                    leveledUp = animationData.leveledUp,
                    newLevel = animationData.newLevel,
                    onAnimationEnd = {
                        viewModel.clearXpAnimation()
                    }
                )
            }
        }

        // Show snackbar for notifications
        uiState.notification?.let { message ->
            // In production, you'd show a Snackbar here
        }
    }
}