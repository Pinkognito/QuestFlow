package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.questflow.presentation.screens.timeline.model.ContextMenuState

/**
 * Persistent menu icon button that appears at finger release position.
 *
 * NEW DESIGN (2025-10-15):
 * - Small icon button appears at exact finger position
 * - Stays visible until next selection
 * - Tap to expand action menu
 * - Compact, non-intrusive design
 */
@Composable
fun SelectionMenuButton(
    state: ContextMenuState,
    onActionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val screenWidth = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }

    var isExpanded by remember { mutableStateOf(false) }

    // Icon button size
    val buttonSize = with(density) { 56.dp.toPx() }

    // Keep button fully on screen
    val safeMargin = buttonSize / 2
    val adjustedX = state.centerX.coerceIn(safeMargin, screenWidth - safeMargin)
    val adjustedY = state.centerY.coerceIn(safeMargin, screenHeight - safeMargin)

    // Animation for button appearance
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Main icon button at finger position
        Box(
            modifier = Modifier
                .offset { IntOffset(adjustedX.toInt(), adjustedY.toInt()) }
                .size(56.dp)
                .scale(scale)
        ) {
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.Center),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.MoreVert,
                    contentDescription = if (isExpanded) "Menü schließen" else "Aktionen öffnen"
                )
            }

            // Expanded action menu (appears above button)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .offset(y = (-16).dp) // Position above button
                        .width(200.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        // Alles auswählen
                        MenuActionItem(
                            icon = Icons.Default.CheckCircle,
                            label = "Alles auswählen",
                            onClick = {
                                onActionSelected("select_all")
                                isExpanded = false
                            }
                        )

                        Divider()

                        // Einfügen
                        MenuActionItem(
                            icon = Icons.Default.Add,
                            label = "Einfügen",
                            onClick = {
                                onActionSelected("insert")
                                isExpanded = false
                            }
                        )

                        Divider()

                        // Löschen
                        MenuActionItem(
                            icon = Icons.Default.Delete,
                            label = "Löschen",
                            color = Color.Red,
                            onClick = {
                                onActionSelected("delete")
                                isExpanded = false
                            }
                        )

                        Divider()

                        // Zeit anpassen
                        MenuActionItem(
                            icon = Icons.Default.Edit,
                            label = "Zeit anpassen",
                            onClick = {
                                onActionSelected("adjust_time")
                                isExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single menu action item
 */
@Composable
private fun MenuActionItem(
    icon: ImageVector,
    label: String,
    color: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
