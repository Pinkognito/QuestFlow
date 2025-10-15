package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.questflow.presentation.screens.timeline.model.ContextMenuAction
import com.example.questflow.presentation.screens.timeline.model.ContextMenuState
import com.example.questflow.presentation.screens.timeline.model.ContextMenuType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Radial context menu that appears at finger release position.
 * User can swipe from center to activate an action.
 *
 * Design:
 * - Center circle at release position
 * - 4 action buttons arranged radially (90° apart)
 * - Swipe gesture detection for activation
 * - Visual feedback for selected action
 */
@Composable
fun RadialContextMenu(
    state: ContextMenuState,
    onActionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val screenWidth = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val buttonRadius = with(density) { 80.dp.toPx() } // Distance from center to buttons
    val buttonSize = with(density) { 64.dp.toPx() } // Button diameter
    val safeMargin = buttonRadius + buttonSize / 2 // Total space needed from edge

    // IMPROVED: Keep buttons on screen by adjusting center position + rotating button layout if needed
    val adjustedX = state.centerX.coerceIn(safeMargin, screenWidth - safeMargin)
    val adjustedY = state.centerY.coerceIn(safeMargin, screenHeight - safeMargin)

    // Detect edge proximity for dynamic button arrangement
    val nearLeftEdge = state.centerX < safeMargin + 30f
    val nearRightEdge = state.centerX > screenWidth - safeMargin - 30f
    val nearTopEdge = state.centerY < safeMargin + 30f
    val nearBottomEdge = state.centerY > screenHeight - safeMargin - 30f

    // Adjust button angles dynamically based on edge proximity
    val angleOffset = when {
        nearLeftEdge && nearTopEdge -> 45f      // Bottom-right quadrant
        nearRightEdge && nearTopEdge -> 135f    // Bottom-left quadrant
        nearRightEdge && nearBottomEdge -> 225f // Top-left quadrant
        nearLeftEdge && nearBottomEdge -> 315f  // Top-right quadrant
        nearLeftEdge -> 90f   // Move buttons to right hemisphere
        nearRightEdge -> 270f // Move buttons to left hemisphere
        nearTopEdge -> 180f   // Move buttons to bottom hemisphere
        nearBottomEdge -> 0f  // Move buttons to top hemisphere
        else -> 0f            // No adjustment needed
    }

    val actions = getActionsForMenuType(state.menuType, state.selectedTasksInBox, angleOffset)

    // Animation for entrance
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "menu_scale"
    )

    // CRITICAL: Use Box WITHOUT fillMaxSize to allow clicks through
    // Only the menu area itself should capture touches
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Detect taps OUTSIDE menu area → dismiss
                detectTapGestures { offset ->
                    val dx = offset.x - state.centerX
                    val dy = offset.y - state.centerY
                    val distance = sqrt(dx * dx + dy * dy)

                    // If tap is far from menu center (> 120dp), dismiss
                    if (distance > with(density) { 120.dp.toPx() }) {
                        android.util.Log.d("RadialMenu", "Outside tap → dismiss")
                        onDismiss()
                    }
                }
            }
    ) {
        android.util.Log.d("RadialMenu", "Position: original=(${state.centerX}, ${state.centerY}), adjusted=($adjustedX, $adjustedY), screen=($screenWidth x $screenHeight), safeMargin=$safeMargin, angleOffset=$angleOffset")

        Box(
            modifier = Modifier
                .offset { IntOffset(adjustedX.toInt(), adjustedY.toInt()) }
        ) {
            // Radial action buttons - TAP instead of SWIPE
            actions.forEach { action ->
                TappableActionButton(
                    action = action,
                    radius = 80.dp,
                    scale = scale,
                    onTap = {
                        android.util.Log.d("RadialMenu", "Button tapped: ${action.id}")
                        onActionSelected(action.id)
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * Tappable action button (replaces swipe gesture)
 */
@Composable
private fun TappableActionButton(
    action: ContextMenuAction,
    radius: androidx.compose.ui.unit.Dp,
    scale: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }

    // Calculate position based on angle
    // 0° = right, 90° = down, 180° = left, 270° = up
    val angleRad = Math.toRadians(action.angle.toDouble())
    val offsetX = (radiusPx * cos(angleRad)).toFloat()
    val offsetY = (radiusPx * sin(angleRad)).toFloat()

    var isPressed by remember { mutableStateOf(false) }

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .offset { IntOffset((offsetX * scale).toInt(), (offsetY * scale).toInt()) }
            .size((64.dp * buttonScale))
    ) {
        Surface(
            onClick = {
                onTap()
            },
            shape = MaterialTheme.shapes.medium,
            color = if (isPressed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = action.color ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Get actions based on menu type with dynamic angle offset for edge positioning
 */
private fun getActionsForMenuType(
    menuType: ContextMenuType,
    tasksInBox: Int,
    angleOffset: Float = 0f
): List<ContextMenuAction> {
    val baseActions = when (menuType) {
        ContextMenuType.SELECTION_WITH_TASKS -> listOf(
            ContextMenuAction(
                id = "insert",
                label = "Einfügen",
                icon = Icons.Default.Add,
                angle = 0f // Right
            ),
            ContextMenuAction(
                id = "details",
                label = "Details",
                icon = Icons.Default.List,
                angle = 90f // Down
            ),
            ContextMenuAction(
                id = "delete",
                label = "Löschen",
                icon = Icons.Default.Delete,
                angle = 180f, // Left
                color = Color.Red
            ),
            ContextMenuAction(
                id = "edit",
                label = "Bearbeiten",
                icon = Icons.Default.Edit,
                angle = 270f // Up
            )
        )

        ContextMenuType.SELECTION_EMPTY -> listOf(
            ContextMenuAction(
                id = "insert",
                label = "Einfügen",
                icon = Icons.Default.Add,
                angle = 0f
            ),
            ContextMenuAction(
                id = "create",
                label = "Erstellen",
                icon = Icons.Default.Create,
                angle = 90f
            ),
            ContextMenuAction(
                id = "cancel",
                label = "Abbrechen",
                icon = Icons.Default.Close,
                angle = 180f
            ),
            ContextMenuAction(
                id = "edit",
                label = "Bearbeiten",
                icon = Icons.Default.Edit,
                angle = 270f
            )
        )

        ContextMenuType.SINGLE_TASK -> listOf(
            ContextMenuAction(
                id = "complete",
                label = "Fertig",
                icon = Icons.Default.Check,
                angle = 0f,
                color = Color.Green
            ),
            ContextMenuAction(
                id = "edit",
                label = "Bearbeiten",
                icon = Icons.Default.Edit,
                angle = 90f
            ),
            ContextMenuAction(
                id = "delete",
                label = "Löschen",
                icon = Icons.Default.Delete,
                angle = 180f,
                color = Color.Red
            ),
            ContextMenuAction(
                id = "copy",
                label = "Kopieren",
                icon = Icons.Default.Share,
                angle = 270f
            )
        )
    }

    // Apply angle offset to rotate button arrangement based on screen edges
    return baseActions.map { it.copy(angle = it.angle + angleOffset) }
}
