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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Radial Tab Switcher for Task Edit Dialog
 *
 * BEHAVIOR (2025-10-22):
 * - Fixed button always visible at bottom
 * - Press & hold → Radial menu appears
 * - Drag in direction → Tab switches IMMEDIATELY (live preview)
 * - Change direction → Different tab loads IMMEDIATELY
 * - Release → Selected tab stays active
 *
 * Based on CalendarRadialButton.kt pattern
 */

/**
 * Tab definition
 */
data class TaskDialogTab(
    val index: Int,
    val label: String,
    val emoji: String,
    val angle: Float
)

/**
 * Radial tab switcher button
 */
@Composable
fun TaskDialogRadialTabSwitcher(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Menu activation state
    var isMenuActive by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hoveredTabIndex by remember { mutableStateOf<Int?>(null) }

    // 5 tabs in radial layout (360° / 5 = 72° spacing)
    val tabs = remember {
        listOf(
            TaskDialogTab(0, "Basis", "📝", 0f),      // Right
            TaskDialogTab(1, "Zeit", "⏰", 72f),      // Upper-Right
            TaskDialogTab(2, "Personen", "👥", 144f), // Upper-Left
            TaskDialogTab(3, "Optionen", "⚙️", 216f), // Lower-Left
            TaskDialogTab(4, "Verlauf", "📜", 288f)   // Lower-Right
        )
    }

    // Animation
    val menuScale by animateFloatAsState(
        targetValue = if (isMenuActive) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "menu_scale"
    )

    // LIVE TAB SWITCHING - Change tab immediately when hovering over option
    LaunchedEffect(hoveredTabIndex) {
        hoveredTabIndex?.let { tabIndex ->
            if (tabIndex != selectedTab) {
                android.util.Log.d("RadialTabSwitcher", "🔄 Live tab switch: $selectedTab → $tabIndex")
                onTabSelected(tabIndex)
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Radial menu overlay (visible when active)
        if (isMenuActive) {
            RadialMenuOverlay(
                tabs = tabs,
                scale = menuScale,
                selectedTab = hoveredTabIndex ?: selectedTab,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Center button (always visible)
        FloatingActionButton(
            onClick = { /* Handled by gesture */ },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            android.util.Log.d("RadialTabSwitcher", "🎮 Button pressed → Menu activated")
                            isMenuActive = true
                            dragOffset = Offset.Zero
                            hoveredTabIndex = null
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            dragOffset += drag

                            val distance = sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)

                            if (distance > 50f) {
                                // Calculate angle from drag offset
                                val angle = atan2(dragOffset.y, dragOffset.x) * 180 / PI.toFloat()
                                val normalizedAngle = ((angle + 360) % 360)

                                // Find closest tab
                                val closestTab = tabs.minByOrNull { tab ->
                                    val diff = abs(normalizedAngle - tab.angle)
                                    min(diff, 360 - diff)
                                }

                                // Update hovered tab (triggers live switch via LaunchedEffect)
                                hoveredTabIndex = closestTab?.index

                                android.util.Log.d("RadialTabSwitcher", "📍 Drag: angle=$normalizedAngle°, hovered=${closestTab?.label}")
                            } else {
                                hoveredTabIndex = null
                            }
                        },
                        onDragEnd = {
                            android.util.Log.d("RadialTabSwitcher", "✅ Released: Final tab = ${hoveredTabIndex ?: selectedTab}")
                            isMenuActive = false
                            dragOffset = Offset.Zero
                            hoveredTabIndex = null
                        },
                        onDragCancel = {
                            android.util.Log.d("RadialTabSwitcher", "❌ Cancelled")
                            isMenuActive = false
                            dragOffset = Offset.Zero
                            hoveredTabIndex = null
                        }
                    )
                },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = tabs[selectedTab].emoji,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Radial menu overlay with tab options
 */
@Composable
private fun RadialMenuOverlay(
    tabs: List<TaskDialogTab>,
    scale: Float,
    selectedTab: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        // Lines connecting to tabs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = 90f * scale

            tabs.forEach { tab ->
                val angleRad = Math.toRadians(tab.angle.toDouble())
                val x = centerX + (radius * cos(angleRad)).toFloat()
                val y = centerY + (radius * sin(angleRad)).toFloat()

                drawLine(
                    color = if (selectedTab == tab.index) Color(0xFF6200EA) else Color(0xFF888888),
                    start = Offset(centerX, centerY),
                    end = Offset(x, y),
                    strokeWidth = if (selectedTab == tab.index) 4f else 2f
                )
            }
        }

        // Tab buttons
        tabs.forEach { tab ->
            RadialTabButton(tab, scale, selectedTab == tab.index, Modifier.align(Alignment.Center))
        }
    }
}

/**
 * Individual tab button in radial menu
 */
@Composable
private fun RadialTabButton(
    tab: TaskDialogTab,
    scale: Float,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val radius = 90f * scale
    val angleRad = Math.toRadians(tab.angle.toDouble())
    val offsetX = (radius * cos(angleRad)).toFloat()
    val offsetY = (radius * sin(angleRad)).toFloat()

    Box(
        modifier = modifier
            .offset { IntOffset((offsetX * with(density) { 1.dp.toPx() }).toInt(), (offsetY * with(density) { 1.dp.toPx() }).toInt()) }
            .size(if (isSelected) 60.dp else 56.dp)
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
                Text(
                    text = tab.emoji,
                    fontSize = if (isSelected) 28.sp else 24.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
