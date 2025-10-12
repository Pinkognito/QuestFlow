package com.example.questflow.presentation.screens.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questflow.presentation.screens.timeline.model.SelectionBox
import java.time.format.DateTimeFormatter

/**
 * Floating control panel for selection box operations.
 * Shows as a fixed card at bottom of screen with action buttons.
 * Can be minimized to not block timeline content.
 */
@Composable
fun SelectionBoxOverlay(
    selectionBox: SelectionBox,
    onDismiss: () -> Unit,
    onSelectAllInRange: () -> Unit,
    onInsertIntoRange: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Control panel at bottom
        SelectionBoxControlPanel(
            selectionBox = selectionBox,
            isExpanded = isExpanded,
            onToggleExpanded = { isExpanded = !isExpanded },
            onDismiss = onDismiss,
            onSelectAllInRange = onSelectAllInRange,
            onInsertIntoRange = onInsertIntoRange,
            onEdit = onEdit,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Control panel showing selection box info and action buttons
 * Can be minimized/expanded
 */
@Composable
private fun SelectionBoxControlPanel(
    selectionBox: SelectionBox,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDismiss: () -> Unit,
    onSelectAllInRange: () -> Unit,
    onInsertIntoRange: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeText = "${selectionBox.startTime.format(timeFormatter)} - ${selectionBox.endTime.format(timeFormatter)}"
    val durationText = "${selectionBox.durationMinutes()}min"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Always visible: Compact header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏱️",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "($durationText)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Row {
                    IconButton(onClick = onToggleExpanded) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isExpanded) "Einklappen" else "Ausklappen",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Expandable content
            if (isExpanded) {
                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSelectAllInRange,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📋 Alle wählen")
                        }

                        Button(
                            onClick = onInsertIntoRange,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✨ Einfügen")
                        }
                    }

                    if (onEdit != null) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zeitbereich bearbeiten")
                        }
                    }
                }
            }
        }
    }
}
