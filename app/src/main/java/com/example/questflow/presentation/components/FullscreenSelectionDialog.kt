package com.example.questflow.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Fullscreen selection dialog that opens from top of screen
 * Prevents keyboard from covering search results
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FullscreenSelectionDialog(
    title: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T?) -> Unit,
    onDismiss: () -> Unit,
    itemLabel: (T) -> String,
    itemDescription: ((T) -> String)? = null,
    allowNone: Boolean = true,
    noneLabel: String = "Keine Auswahl",
    searchPlaceholder: String = "Suchen..."
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var searchQuery by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        // Filter items based on search
        val filteredItems = remember(items, searchQuery) {
            if (searchQuery.isBlank()) {
                items
            } else {
                items.filter { item ->
                    itemLabel(item).contains(searchQuery, ignoreCase = true) ||
                    (itemDescription?.invoke(item)?.contains(searchQuery, ignoreCase = true) == true)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Bar
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Schließen")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text(searchPlaceholder) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Leeren")
                            }
                        }
                    },
                    singleLine = true
                )

                // Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding() // Prevents keyboard from covering content
                ) {
                    // "None" option
                    if (allowNone) {
                        item {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        noneLabel,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (selectedItem == null) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onItemSelected(null)
                                    onDismiss()
                                },
                                leadingContent = {
                                    RadioButton(
                                        selected = selectedItem == null,
                                        onClick = {
                                            onItemSelected(null)
                                            onDismiss()
                                        }
                                    )
                                }
                            )
                            HorizontalDivider()
                        }
                    }

                    // Filtered items
                    items(filteredItems) { item ->
                        val isSelected = selectedItem == item
                        ListItem(
                            headlineContent = {
                                Text(
                                    itemLabel(item),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            supportingContent = itemDescription?.let { desc ->
                                { Text(desc(item), style = MaterialTheme.typography.bodySmall) }
                            },
                            modifier = Modifier.clickable {
                                onItemSelected(item)
                                onDismiss()
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onItemSelected(item)
                                        onDismiss()
                                    }
                                )
                            }
                        )
                        HorizontalDivider()
                    }

                    // Empty state
                    if (filteredItems.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Keine Ergebnisse gefunden",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Auto-focus search field
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }
}
