package com.example.questflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.data.database.entity.CategoryEntity

@Composable
fun CategoryDropdown(
    selectedCategory: CategoryEntity?,
    categories: List<CategoryEntity>,
    onCategorySelected: (CategoryEntity?) -> Unit,
    onManageCategoriesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Selected category display
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedCategory != null) {
                    // Category emoji
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(selectedCategory.color))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedCategory.emoji,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = selectedCategory.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Alle",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "Alle" option
            DropdownMenuItem(
                text = {
                    Text(
                        "Alle Kategorien",
                        fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )

            HorizontalDivider()

            // Categories
            categories.forEach { category ->
                DropdownMenuItem(
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    try {
                                        Color(android.graphics.Color.parseColor(category.color))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.emoji,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                category.name,
                                fontWeight = if (selectedCategory?.id == category.id) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                "Level ${category.currentLevel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }

            HorizontalDivider()

            // Manage categories option
            DropdownMenuItem(
                leadingIcon = {
                    Icon(Icons.Default.Settings, contentDescription = null)
                },
                text = { Text("Kategorien verwalten") },
                onClick = {
                    onManageCategoriesClick()
                    expanded = false
                }
            )
        }
    }
}