package com.example.questflow.presentation.screens.statistics.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.model.*

@Composable
fun WidgetPickerDialog(
    onDismiss: () -> Unit,
    onWidgetSelected: (WidgetTemplate) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Widget hinzufügen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(WidgetTemplates.templates) { template ->
                    WidgetTemplateCard(
                        template = template,
                        onClick = { onWidgetSelected(template) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
fun WidgetTemplateCard(
    template: WidgetTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (template.icon) {
                    "check_circle" -> Icons.Default.CheckCircle
                    "star" -> Icons.Default.Star
                    "local_fire_department" -> Icons.Default.Refresh // Closest match
                    "trending_up" -> Icons.Default.Share // Closest match
                    "calendar_today" -> Icons.Default.DateRange
                    "grade" -> Icons.Default.Star
                    "show_chart" -> Icons.Default.Info
                    "bar_chart" -> Icons.Default.Info
                    "pie_chart" -> Icons.Default.Info
                    "speed" -> Icons.Default.Refresh
                    "priority_high" -> Icons.Default.Warning
                    "source" -> Icons.Default.Star
                    "grid_on" -> Icons.Default.Menu
                    "linear_scale" -> Icons.Default.Menu
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = template.defaultTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Text(
                text = template.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
