package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.questflow.presentation.screens.timeline.model.ContextMenuState
import kotlin.math.*

/**
 * Radial Joystick Menu - Press button and swipe to select action
 *
 * DESIGN (2025-10-15 v3 - BUTTON-BASED):
 * - Round button appears at finger position
 * - Press button → Radial menu appears
 * - Swipe in direction → Select action
 * - Release → Execute action
 */
@Composable
fun RadialJoystickMenu(
    state: ContextMenuState,
    onActionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val screenWidth = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }

    // Menu activation state
    var isMenuActive by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedAction by remember { mutableStateOf<String?>(null) }

    // Button positioning - CENTER at finger (no offset!)
    val buttonRadius = with(density) { 28.dp.toPx() }
    val headerHeightPx = with(density) { 48.dp.toPx() } // Timeline header height

    // CRITICAL FIX: Center button exactly at finger position, adjusted for header
    val buttonCenterX = state.centerX.coerceIn(buttonRadius, screenWidth - buttonRadius)
    val buttonCenterY = (state.centerY - headerHeightPx).coerceIn(buttonRadius, screenHeight - buttonRadius)

    // Convert center to top-left for offset positioning
    val buttonX = buttonCenterX - buttonRadius
    val buttonY = buttonCenterY - buttonRadius

    android.util.Log.d("RadialJoystick", "🎯 Finger=(${state.centerX.toInt()}, ${state.centerY.toInt()}), ButtonCenter=(${buttonCenterX.toInt()}, ${buttonCenterY.toInt()}), ButtonTopLeft=(${buttonX.toInt()}, ${buttonY.toInt()})")

    // Actions (4 directions)
    val actions = listOf(
        RadialAction(id = "select_all", label = "Alles auswählen", icon = Icons.Default.CheckCircle, angle = 0f),
        RadialAction(id = "insert", label = "Einfügen", icon = Icons.Default.Add, angle = 90f),
        RadialAction(id = "delete", label = "Löschen", icon = Icons.Default.Delete, angle = 180f, color = Color.Red),
        RadialAction(id = "adjust_time", label = "Zeit anpassen", icon = Icons.Default.Edit, angle = 270f)
    )

    // Animation
    val menuScale by animateFloatAsState(
        targetValue = if (isMenuActive) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "menu_scale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Button at finger position
        Box(
            modifier = Modifier
                .offset { IntOffset(buttonX.toInt(), buttonY.toInt()) }
                .size(56.dp)
        ) {
            // Radial menu overlay (visible when active)
            if (isMenuActive) {
                RadialMenuOverlay(
                    actions = actions,
                    scale = menuScale,
                    selectedAction = selectedAction,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Center button
            FloatingActionButton(
                onClick = { /* Handled by gesture */ },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                android.util.Log.d("RadialJoystick", "🎮 Button pressed → Menu activated")
                                isMenuActive = true
                                dragOffset = Offset.Zero
                                selectedAction = null
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                dragOffset += drag

                                val distance = sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)
                                if (distance > 50f) {
                                    val angle = atan2(dragOffset.y, dragOffset.x) * 180 / PI
                                    selectedAction = getActionByAngle(angle.toFloat(), actions)
                                    android.util.Log.d("RadialJoystick", "📍 Swipe: angle=$angle°, selected=$selectedAction")
                                } else {
                                    selectedAction = null
                                }
                            },
                            onDragEnd = {
                                android.util.Log.d("RadialJoystick", "✅ Released: $selectedAction")
                                selectedAction?.let { onActionSelected(it) }
                                isMenuActive = false
                                dragOffset = Offset.Zero
                                selectedAction = null
                            },
                            onDragCancel = {
                                android.util.Log.d("RadialJoystick", "❌ Cancelled")
                                isMenuActive = false
                                dragOffset = Offset.Zero
                                selectedAction = null
                            }
                        )
                    },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Aktionen")
            }
        }
    }
}

/**
 * Radial menu overlay
 */
@Composable
private fun RadialMenuOverlay(
    actions: List<RadialAction>,
    scale: Float,
    selectedAction: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        // Lines and buttons
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = 80f * scale

            actions.forEach { action ->
                val angleRad = Math.toRadians(action.angle.toDouble())
                val x = centerX + (radius * cos(angleRad)).toFloat()
                val y = centerY + (radius * sin(angleRad)).toFloat()

                drawLine(
                    color = if (selectedAction == action.id) Color(0xFF6200EA) else Color(0xFF888888),
                    start = Offset(centerX, centerY),
                    end = Offset(x, y),
                    strokeWidth = if (selectedAction == action.id) 4f else 2f
                )
            }
        }

        // Buttons
        actions.forEach { action ->
            RadialActionButton(action, scale, selectedAction == action.id, Modifier.align(Alignment.Center))
        }
    }
}

/**
 * Action button
 */
@Composable
private fun RadialActionButton(
    action: RadialAction,
    scale: Float,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val radius = 80f * scale
    val angleRad = Math.toRadians(action.angle.toDouble())
    val offsetX = (radius * cos(angleRad)).toFloat()
    val offsetY = (radius * sin(angleRad)).toFloat()

    Box(
        modifier = modifier
            .offset { IntOffset((offsetX * with(density) { 1.dp.toPx() }).toInt(), (offsetY * with(density) { 1.dp.toPx() }).toInt()) }
            .size(if (isSelected) 52.dp else 48.dp)
            .scale(scale)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            tonalElevation = if (isSelected) 8.dp else 4.dp,
            shadowElevation = if (isSelected) 12.dp else 6.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else (action.color ?: MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Get action by angle
 */
private fun getActionByAngle(angle: Float, actions: List<RadialAction>): String? {
    val normalizedAngle = ((angle + 360) % 360)
    return actions.minByOrNull { action ->
        val diff = abs(normalizedAngle - action.angle)
        min(diff, 360 - diff)
    }?.id
}

/**
 * Action definition
 */
private data class RadialAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val angle: Float,
    val color: Color? = null
)
