package com.example.questflow.presentation.screens.library

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.components.QuestFlowTopBar
import com.example.questflow.presentation.viewmodels.MetadataLibraryViewModel

/**
 * Modern Grid-based Library Overview Screen
 * Shows all library categories as clickable cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    appViewModel: AppViewModel,
    navController: NavController,
    metadataViewModel: MetadataLibraryViewModel = hiltViewModel()
) {
    val selectedCategory by appViewModel.selectedCategory.collectAsState()
    val categories by appViewModel.categories.collectAsState()
    val globalStats by appViewModel.globalStats.collectAsState()

    var previousXp by remember { mutableStateOf(globalStats?.xp ?: 0L) }

    // Track XP changes for animation
    LaunchedEffect(globalStats?.xp) {
        globalStats?.xp?.let { currentXp ->
            if (currentXp != previousXp) {
                previousXp = currentXp
            }
        }
    }

    // Get counts for each library type
    val locations by metadataViewModel.locations.collectAsState()
    val contacts by metadataViewModel.contacts.collectAsState()
    val phones by metadataViewModel.phoneNumbers.collectAsState()
    val addresses by metadataViewModel.addresses.collectAsState()
    val emails by metadataViewModel.emails.collectAsState()
    val urls by metadataViewModel.urls.collectAsState()
    val notes by metadataViewModel.notes.collectAsState()
    val files by metadataViewModel.files.collectAsState()

    val libraryCategories = listOf(
        LibraryCategory(
            title = "Media",
            description = "Bilder & GIFs",
            icon = Icons.Default.Star,
            route = "media_library",
            count = null // Media count could be added later
        ),
        LibraryCategory(
            title = "Textbausteine",
            description = "Templates & Vorlagen",
            icon = Icons.Default.Create,
            route = "text_templates",
            count = null // Count could be added later
        ),
        LibraryCategory(
            title = "Tags",
            description = "Globale Tags verwalten",
            icon = Icons.Default.Settings,
            route = "tag_management",
            count = null // Count could be added later
        ),
        LibraryCategory(
            title = "Standorte",
            description = "Orte & Adressen",
            icon = Icons.Default.LocationOn,
            route = "library_locations",
            count = locations.size
        ),
        LibraryCategory(
            title = "Kontakte",
            description = "Personen",
            icon = Icons.Default.Person,
            route = "library_contacts",
            count = contacts.size
        ),
        LibraryCategory(
            title = "Telefone",
            description = "Nummern",
            icon = Icons.Default.Phone,
            route = "library_phones",
            count = phones.size
        ),
        LibraryCategory(
            title = "Adressen",
            description = "Postanschriften",
            icon = Icons.Default.Home,
            route = "library_addresses",
            count = addresses.size
        ),
        LibraryCategory(
            title = "E-Mails",
            description = "E-Mail-Adressen",
            icon = Icons.Default.Email,
            route = "library_emails",
            count = emails.size
        ),
        LibraryCategory(
            title = "Links",
            description = "URLs",
            icon = Icons.Default.Star,
            route = "library_urls",
            count = urls.size
        ),
        LibraryCategory(
            title = "Notizen",
            description = "Textnotizen",
            icon = Icons.Default.Info,
            route = "library_notes",
            count = notes.size
        ),
        LibraryCategory(
            title = "Dateien",
            description = "Anhänge",
            icon = Icons.Default.Settings,
            route = "library_files",
            count = files.size
        ),
        LibraryCategory(
            title = "Einstellungen",
            description = "System & Backup",
            icon = Icons.Default.Settings,
            route = "settings",
            count = null
        )
    )

    Scaffold(
        topBar = {
            QuestFlowTopBar(
                title = "Bibliothek",
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = appViewModel::selectCategory,
                onManageCategoriesClick = {
                    navController.navigate("categories")
                },
                level = globalStats?.level ?: 1,
                totalXp = globalStats?.xp ?: 0,
                previousXp = previousXp
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(libraryCategories) { category ->
                LibraryCategoryCard(
                    category = category,
                    onClick = { navController.navigate(category.route) }
                )
            }
        }
    }
}

private data class LibraryCategory(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val count: Int?
)

@Composable
private fun LibraryCategoryCard(
    category: LibraryCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            category.count?.let { count ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$count Einträge",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
