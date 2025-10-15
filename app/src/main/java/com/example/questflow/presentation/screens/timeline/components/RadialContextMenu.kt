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
 * Radial context menu that appears near finger release position.
 *
 * FIXED (2025-10-15 v2):
 * - Buttons always centered at finger position (or as close as possible)
 * - All buttons stay on screen
 * - Menu dismisses automatically when clicking outside dismiss area
 * - Non-blocking: rest of screen remains interactive
 *
 * Design:
 * - 4 action buttons arranged radially around finger (90° apart)
 * - Smart edge adjustment: buttons shift toward screen center when near edges
 * - Tap gesture for activation
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

    // Menu dimensions
    val buttonRadius = with(density) { 80.dp.toPx() } // Distance from center to buttons
    val buttonSize = with(density) { 64.dp.toPx() }   // Button diameter
    val safeMargin = buttonRadius + buttonSize / 2    // Total space needed from edge

    // POSITIONING: Keep menu center as close to finger as possible while keeping all buttons visible
    val minX = safeMargin
    val maxX = screenWidth - safeMargin
    val minY = safeMargin
    val maxY = screenHeight - safeMargin

    // Adjust center to keep all buttons on screen
    val adjustedX = state.centerX.coerceIn(minX, maxX)
    val adjustedY = state.centerY.coerceIn(minY, maxY)

    // Calculate shift for logging
    val shiftX = adjustedX - state.centerX
    val shiftY = adjustedY - state.centerY
    val totalShift = sqrt(shiftX * shiftX + shiftY * shiftY)

    android.util.Log.d("RadialMenu", "🎯 Positioning: finger=(${state.centerX.toInt()}, ${state.centerY.toInt()}), adjusted=(${"%.0f".format(adjustedX)}, ${"%.0f".format(adjustedY)}), shift=${"%.0f".format(totalShift)}px")

    // Get actions
    val actions = getActionsForMenuType(state.menuType, state.selectedTasksInBox)

    // Animation
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "menu_scale"
    )

    // PARENT BOX: Needed for absolute positioning within screen
    // Uses fillMaxSize to allow offset positioning
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // MENU CONTENT: Positioned at adjusted center
        Box(
            modifier = Modifier
                .offset { IntOffset(adjustedX.toInt(), adjustedY.toInt()) }
        ) {
            // DISMISS AREA: Invisible box that catches taps around menu
            Box(
                modifier = Modifier
                    .size(with(density) { (safeMargin * 2.5f).toDp() }) // 2.5x = comfortable dismiss area
                    .align(Alignment.Center)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Calculate distance from center
                            val centerPx = size.width / 2f
                            val dx = offset.x - centerPx
                            val dy = offset.y - centerPx
                            val distance = sqrt(dx * dx + dy * dy)

                            // Dismiss if tap is outside menu proper (but inside dismiss area)
                            val menuProperRadius = with(density) { (buttonRadius + buttonSize / 2).toDp().toPx() }
                            if (distance > menuProperRadius) {
                                android.util.Log.d("RadialMenu", "🚫 Tap outside menu → dismiss")
                                onDismiss()
                            }
                        }
                    }
            )

            // ACTION BUTTONS: Arranged radially around center
            actions.forEach { action ->
                TappableActionButton(
                    action = action,
                    radius = 80.dp,
                    scale = scale,
                    onTap = {
                        android.util.Log.d("RadialMenu", "✅ Button tapped: ${action.id}")
                        onActionSelected(action.id)
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * Tappable action button
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
 * Get actions based on menu type
 */
private fun getActionsForMenuType(
    menuType: ContextMenuType,
    tasksInBox: Int
): List<ContextMenuAction> {
    return when (menuType) {
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
}
