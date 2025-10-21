package com.example.questflow.presentation.components

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
import java.time.LocalDate
import kotlin.math.*

/**
 * Calendar Radial Button Menu - EXACT COPY of Timeline RadialJoystickMenu pattern
 *
 * Press button at calendar cell → Radial menu appears → Swipe → Release → Execute action
 */

/**
 * State for calendar radial button
 */
data class CalendarRadialButtonState(
    val centerX: Float,
    val centerY: Float,
    val targetDate: LocalDate,
    val isActive: Boolean = false
)

/**
 * Calendar actions
 */
sealed class CalendarButtonAction {
    object SetAsStart : CalendarButtonAction()
    object SetAsEnd : CalendarButtonAction()
    object ChangeStartTime : CalendarButtonAction()
    object ChangeEndTime : CalendarButtonAction()
}

/**
 * Radial button menu for calendar - EXACT pattern from Timeline
 */
@Composable
fun CalendarRadialButton(
    state: CalendarRadialButtonState,
    onActionSelected: (CalendarButtonAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val screenWidth = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }

    android.util.Log.d("CalendarRadialButton", "═══════════════════════════════════════")
    android.util.Log.d("CalendarRadialButton", "🎯 BUTTON RENDERED")
    android.util.Log.d("CalendarRadialButton", "   Finger: (${state.centerX.toInt()}, ${state.centerY.toInt()})")
    android.util.Log.d("CalendarRadialButton", "   Screen: ${screenWidth.toInt()} x ${screenHeight.toInt()}")
    android.util.Log.d("CalendarRadialButton", "   Active: ${state.isActive}")

    // Menu activation state
    var isMenuActive by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedAction by remember { mutableStateOf<String?>(null) }

    // Button positioning - CENTER at finger
    val buttonRadius = with(density) { 28.dp.toPx() }

    // NO OFFSET NEEDED! Popup renders in window coordinates, same as positionInRoot()
    // Just coerce to screen bounds
    val buttonCenterX = state.centerX.coerceIn(buttonRadius, screenWidth - buttonRadius)
    val buttonCenterY = state.centerY.coerceIn(buttonRadius, screenHeight - buttonRadius)

    // Convert center to top-left for offset positioning
    val buttonX = buttonCenterX - buttonRadius
    val buttonY = buttonCenterY - buttonRadius

    android.util.Log.d("CalendarRadialButton", "   ButtonCenter: (${buttonCenterX.toInt()}, ${buttonCenterY.toInt()})")
    android.util.Log.d("CalendarRadialButton", "   ButtonTopLeft: (${buttonX.toInt()}, ${buttonY.toInt()})")
    android.util.Log.d("CalendarRadialButton", "═══════════════════════════════════════")

    // Actions (4 directions) - 0°, 90°, 180°, 270°
    val actions = listOf(
        RadialAction(id = "set_start", label = "Als Start", icon = Icons.Default.DateRange, angle = 0f),
        RadialAction(id = "edit_start_time", label = "Startzeit", icon = Icons.Default.Edit, angle = 90f),
        RadialAction(id = "set_end", label = "Als Ende", icon = Icons.Default.DateRange, angle = 180f),
        RadialAction(id = "edit_end_time", label = "Endzeit", icon = Icons.Default.Edit, angle = 270f)
    )

    // Animation
    val menuScale by animateFloatAsState(
        targetValue = if (isMenuActive) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "menu_scale"
    )

    LaunchedEffect(isMenuActive) {
        android.util.Log.d("CalendarRadialButton", "🎨 Menu active state changed: $isMenuActive")
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Button at finger position
        Box(
            modifier = Modifier
                .offset { IntOffset(buttonX.toInt(), buttonY.toInt()) }
                .size(56.dp)
        ) {
            // Radial menu overlay (visible when active)
            if (isMenuActive) {
                android.util.Log.d("CalendarRadialButton", "📊 Rendering RadialMenuOverlay with scale=$menuScale")
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
                        android.util.Log.d("CalendarRadialButton", "🎮 pointerInput attached to button")
                        detectDragGestures(
                            onDragStart = { offset ->
                                android.util.Log.d("CalendarRadialButton", "═══════════════════════════════════════")
                                android.util.Log.d("CalendarRadialButton", "🎮 onDragStart triggered")
                                android.util.Log.d("CalendarRadialButton", "   offset: $offset")
                                isMenuActive = true
                                dragOffset = Offset.Zero
                                selectedAction = null
                                android.util.Log.d("CalendarRadialButton", "   → Menu activated")
                                android.util.Log.d("CalendarRadialButton", "═══════════════════════════════════════")
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                dragOffset += drag
                                val distance = sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)

                                android.util.Log.d("CalendarRadialButton", "👆 onDrag: drag=$drag, total=$dragOffset, distance=$distance")

                                if (distance > 50f) {
                                    val angle = atan2(dragOffset.y, dragOffset.x) * 180 / PI
                                    selectedAction = getActionByAngle(angle.toFloat(), actions)
                                    android.util.Log.d("CalendarRadialButton", "   → angle=$angle°, selected=$selectedAction")
                                } else {
                                    if (selectedAction != null) {
                                        android.util.Log.d("CalendarRadialButton", "   → Too close to center, deselected")
                                    }
                                    selectedAction = null
                                }
                            },
                            onDragEnd = {
                                android.util.Log.d("CalendarRadialButton", "═══════════════════════════════════════")
                                android.util.Log.d("CalendarRadialButton", "✅ onDragEnd: selectedAction=$selectedAction")

                                selectedAction?.let { actionId ->
                                    val action = when (actionId) {
                                        "set_start" -> CalendarButtonAction.SetAsStart
                                        "set_end" -> CalendarButtonAction.SetAsEnd
                                        "edit_start_time" -> CalendarButtonAction.ChangeStartTime
                                        "edit_end_time" -> CalendarButtonAction.ChangeEndTime
                                        else -> null
                                    }
                                    action?.let {
                                        android.util.Log.d("CalendarRadialButton", "   → Executing action: $it")
                                        onActionSelected(it)
                                    }
                                }

                                isMenuActive = false
                                dragOffset = Offset.Zero
                                selectedAction = null
                                android.util.Log.d("CalendarRadialButton", "   → Menu deactivated")
                                android.util.Log.d("CalendarRadialButton", "═══════════════════════════════════════")
                            },
                            onDragCancel = {
                                android.util.Log.d("CalendarRadialButton", "❌ onDragCancel")
                                isMenuActive = false
                                dragOffset = Offset.Zero
                                selectedAction = null
                            }
                        )
                    },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Kalender Aktionen")
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
