package com.example.questflow.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.data.database.entity.SkillEdgeEntity
import com.example.questflow.data.repository.SkillNodeWithStatus
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SkillTreeCanvas(
    skills: List<SkillNodeWithStatus>,
    edges: List<SkillEdgeEntity>,
    selectedSkillId: String?,
    editMode: Boolean = false,
    onSkillClick: (String) -> Unit,
    onSkillPositionChange: (String, Float, Float) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val nodeRadius = with(density) { 30.dp.toPx() } // Reduced from 40dp to 30dp
    val skillPositions = remember(skills) {
        mutableStateMapOf<String, Offset>().apply {
            putAll(skills.associate { skill ->
                skill.node.id to Offset(
                    skill.node.positionX * density.density,
                    skill.node.positionY * density.density
                )
            })
        }
    }

    var draggedSkillId by remember { mutableStateOf<String?>(null) }

    // Calculate canvas size based on skill positions
    val canvasWidth = remember(skills) {
        val maxX = skillPositions.values.maxOfOrNull { it.x } ?: 0f
        max(1080f, maxX + 200f)
    }
    val canvasHeight = remember(skills) {
        val maxY = skillPositions.values.maxOfOrNull { it.y } ?: 0f
        max(1200f, maxY + 300f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
    ) {
        // Canvas for connections
        Canvas(
            modifier = Modifier
                .width(with(density) { canvasWidth.toDp() })
                .height(with(density) { canvasHeight.toDp() })
                .pointerInput(editMode, skills) {
                    if (editMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Find skill at this position
                                draggedSkillId = skills.find { skill ->
                                    val pos = skillPositions.get(skill.node.id) ?: return@detectDragGestures
                                    val dx = offset.x - pos.x
                                    val dy = offset.y - pos.y
                                    sqrt(dx * dx + dy * dy) < nodeRadius
                                }?.node?.id
                            },
                            onDrag = { change, dragAmount ->
                                draggedSkillId?.let { id ->
                                    val currentPos = skillPositions.get(id) ?: return@let
                                    val newPos = currentPos + dragAmount
                                    skillPositions.put(id, newPos)
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                draggedSkillId?.let { id ->
                                    val pos = skillPositions.get(id) ?: return@let
                                    onSkillPositionChange(
                                        id,
                                        pos.x / density.density,
                                        pos.y / density.density
                                    )
                                }
                                draggedSkillId = null
                            }
                        )
                    } else {
                        detectTapGestures { offset ->
                            // Find skill at tap position
                            skills.find { skill ->
                                val pos = skillPositions.get(skill.node.id) ?: return@detectTapGestures
                                val dx = offset.x - pos.x
                                val dy = offset.y - pos.y
                                sqrt(dx * dx + dy * dy) < nodeRadius
                            }?.let { skill ->
                                onSkillClick(skill.node.id)
                            }
                        }
                    }
                }
        ) {
            // Draw connection lines
            edges.forEach { edge ->
                val parentPos = skillPositions.get(edge.parentId)
                val childPos = skillPositions.get(edge.childId)

                if (parentPos != null && childPos != null) {
                    val parentSkill = skills.find { it.node.id == edge.parentId }
                    val childSkill = skills.find { it.node.id == edge.childId }

                    // Determine line color based on availability
                    val lineColor = when {
                        childSkill?.isAvailable == true -> Color(0xFF4CAF50) // Green - available
                        parentSkill?.isUnlocked == true -> Color(0xFFFFC107) // Amber - parent unlocked
                        else -> Color(0xFF9E9E9E) // Gray - locked
                    }

                    // Calculate direction from parent to child
                    val dx = childPos.x - parentPos.x
                    val dy = childPos.y - parentPos.y
                    val distance = sqrt(dx * dx + dy * dy)
                    val angle = atan2(dy, dx)

                    // Calculate start and end points (offset by node radius)
                    val startX = parentPos.x + nodeRadius * cos(angle)
                    val startY = parentPos.y + nodeRadius * sin(angle)
                    val endX = childPos.x - nodeRadius * cos(angle)
                    val endY = childPos.y - nodeRadius * sin(angle)

                    // Draw curved line
                    val path = Path().apply {
                        moveTo(startX, startY)

                        // Calculate control point for bezier curve
                        val midX = (startX + endX) / 2
                        val midY = (startY + endY) / 2
                        val controlX = midX + (endY - startY) * 0.2f
                        val controlY = midY - (endX - startX) * 0.2f

                        quadraticBezierTo(
                            controlX, controlY,
                            endX, endY
                        )
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Draw arrow at the end
                    val arrowSize = 15.dp.toPx()
                    // Calculate arrow angle from the last segment
                    val arrowAngle = angle

                    val arrowPath = Path().apply {
                        moveTo(endX, endY)
                        lineTo(
                            (endX - arrowSize * cos(arrowAngle - 0.4)).toFloat(),
                            (endY - arrowSize * sin(arrowAngle - 0.4)).toFloat()
                        )
                        moveTo(endX, endY)
                        lineTo(
                            (endX - arrowSize * cos(arrowAngle + 0.4)).toFloat(),
                            (endY - arrowSize * sin(arrowAngle + 0.4)).toFloat()
                        )
                    }

                    drawPath(
                        path = arrowPath,
                        color = lineColor,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Draw investment requirement label at midpoint
                    val midX = (parentPos.x + childPos.x) / 2
                    val midY = (parentPos.y + childPos.y) / 2

                    // Draw background circle for requirement number
                    drawCircle(
                        color = Color.White,
                        radius = 16.dp.toPx(),
                        center = Offset(midX, midY)
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 16.dp.toPx(),
                        center = Offset(midX, midY),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw requirement number
                    val requirementText = edge.minParentInvestment.toString()
                    val textLayoutResult = textMeasurer.measure(
                        text = requirementText,
                        style = TextStyle(
                            color = lineColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            midX - textLayoutResult.size.width / 2,
                            midY - textLayoutResult.size.height / 2
                        )
                    )
                }
            }

            // Draw skill nodes
            skills.forEach { skill ->
                val position = skillPositions.get(skill.node.id) ?: return@forEach

                val nodeColor = when {
                    skill.node.id == selectedSkillId -> Color(0xFF2196F3) // Blue - selected
                    skill.currentInvestment >= skill.node.maxInvestment -> Color(0xFFFFD700) // Gold - maxed
                    skill.isUnlocked -> Color(0xFF4CAF50) // Green - unlocked
                    skill.isAvailable -> Color(0xFF03A9F4) // Light blue - available
                    else -> Color(0xFF757575) // Gray - locked
                }

                // Parse custom color if available
                val customColor = try {
                    Color(android.graphics.Color.parseColor(skill.node.colorHex))
                } catch (e: Exception) {
                    nodeColor
                }

                // Draw outer glow for selected
                if (skill.node.id == selectedSkillId) {
                    drawCircle(
                        color = customColor.copy(alpha = 0.3f),
                        radius = nodeRadius + 10.dp.toPx(),
                        center = position
                    )
                }

                // Draw node circle
                drawCircle(
                    color = customColor,
                    radius = nodeRadius,
                    center = position
                )

                // Draw border
                drawCircle(
                    color = Color.White,
                    radius = nodeRadius,
                    center = position,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw investment progress ring
                if (skill.isUnlocked && skill.node.maxInvestment > 1) {
                    val progress = skill.currentInvestment.toFloat() / skill.node.maxInvestment
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(position.x - nodeRadius - 5.dp.toPx(), position.y - nodeRadius - 5.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            (nodeRadius + 5.dp.toPx()) * 2,
                            (nodeRadius + 5.dp.toPx()) * 2
                        ),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
        }

        // Overlay: Skill labels (using Compose, not Canvas)
        skills.forEach { skill ->
            val position = skillPositions.get(skill.node.id) ?: return@forEach

            SkillNodeLabel(
                skill = skill,
                position = position,
                nodeRadius = nodeRadius,
                modifier = Modifier
            )
        }
    }
}

@Composable
fun SkillNodeLabel(
    skill: SkillNodeWithStatus,
    position: Offset,
    nodeRadius: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .offset(
                x = with(density) { (position.x - nodeRadius).toDp() },
                y = with(density) { (position.y + nodeRadius + 10.dp.toPx()).toDp() }
            )
            .width(with(density) { (nodeRadius * 2).toDp() })
    ) {
        androidx.compose.material3.Text(
            text = skill.node.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
