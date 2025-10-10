package com.example.questflow.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Platzhalter-Kategorie mit Icon und Feldern
 */
data class PlaceholderCategory(
    val name: String,
    val icon: ImageVector,
    val fields: List<PlaceholderField>
)

/**
 * Platzhalter-Feld mit Label und Template-String
 */
data class PlaceholderField(
    val label: String,
    val placeholder: String
)

/**
 * Vordefinierte Platzhalter-Kategorien
 */
val placeholderCategories = listOf(
    PlaceholderCategory(
        name = "Kontakt",
        icon = Icons.Default.Person,
        fields = listOf(
            PlaceholderField("Vollständiger Name", "{kontakt.name}"),
            PlaceholderField("Vorname", "{kontakt.vorname}"),
            PlaceholderField("Nachname", "{kontakt.nachname}"),
            PlaceholderField("Organisation", "{kontakt.organisation}"),
            PlaceholderField("Position", "{kontakt.position}"),
            PlaceholderField("Telefon", "{kontakt.telefon}"),
            PlaceholderField("E-Mail", "{kontakt.email}")
        )
    ),
    PlaceholderCategory(
        name = "Task",
        icon = Icons.Default.CheckCircle,
        fields = listOf(
            PlaceholderField("Titel", "{task.titel}"),
            PlaceholderField("Beschreibung", "{task.beschreibung}"),
            PlaceholderField("Fälligkeitsdatum", "{task.faellig}"),
            PlaceholderField("Priorität", "{task.prioritaet}"),
            PlaceholderField("Kategorie", "{task.kategorie}")
        )
    ),
    PlaceholderCategory(
        name = "Standort",
        icon = Icons.Default.LocationOn,
        fields = listOf(
            PlaceholderField("Name", "{standort.name}"),
            PlaceholderField("Straße", "{standort.strasse}"),
            PlaceholderField("Stadt", "{standort.stadt}"),
            PlaceholderField("PLZ", "{standort.plz}"),
            PlaceholderField("Land", "{standort.land}")
        )
    ),
    PlaceholderCategory(
        name = "Datum & Zeit",
        icon = Icons.Default.DateRange,
        fields = listOf(
            PlaceholderField("Heute", "{datum.heute}"),
            PlaceholderField("Morgen", "{datum.morgen}"),
            PlaceholderField("Gestern", "{datum.gestern}"),
            PlaceholderField("Wochentag", "{datum.wochentag}"),
            PlaceholderField("Aktuelle Uhrzeit", "{zeit.jetzt}"),
            PlaceholderField("Kalenderwoche", "{datum.kw}")
        )
    ),
    PlaceholderCategory(
        name = "Benutzer",
        icon = Icons.Default.AccountCircle,
        fields = listOf(
            PlaceholderField("Mein Name", "{benutzer.name}"),
            PlaceholderField("Meine E-Mail", "{benutzer.email}"),
            PlaceholderField("Meine Signatur", "{benutzer.signatur}")
        )
    )
)

/**
 * Dialog zur hierarchischen Auswahl von Platzhaltern
 *
 * @param onDismiss Wird aufgerufen, wenn der Dialog geschlossen wird
 * @param onPlaceholderSelected Wird mit dem ausgewählten Platzhalter aufgerufen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderSelectorDialog(
    onDismiss: () -> Unit,
    onPlaceholderSelected: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<PlaceholderCategory?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedCategory == null) "Platzhalter auswählen"
                           else selectedCategory!!.name
                )
                if (selectedCategory != null) {
                    IconButton(
                        onClick = { selectedCategory = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                if (selectedCategory == null) {
                    // Zeige Kategorien
                    items(placeholderCategories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = { selectedCategory = category }
                        )
                    }
                } else {
                    // Zeige Felder der ausgewählten Kategorie
                    items(selectedCategory!!.fields) { field ->
                        PlaceholderFieldCard(
                            field = field,
                            onClick = {
                                onPlaceholderSelected(field.placeholder)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun CategoryCard(
    category: PlaceholderCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaceholderFieldCard(
    field: PlaceholderField,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = field.placeholder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
