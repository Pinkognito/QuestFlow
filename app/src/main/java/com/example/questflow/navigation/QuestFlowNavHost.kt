package com.example.questflow.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.screens.tasks.TasksScreen
import com.example.questflow.presentation.screens.collection.CollectionScreen
import com.example.questflow.presentation.screens.collection.CollectionManageScreen
import com.example.questflow.presentation.screens.skilltree.SkillTreeManagementScreen
import com.example.questflow.presentation.screens.categories.CategoriesScreen
import com.example.questflow.presentation.screens.medialibrary.MediaLibraryScreen
import com.example.questflow.presentation.screens.statistics.StatisticsScreen
import com.example.questflow.presentation.screens.metadata.MetadataLibraryScreen
import com.example.questflow.presentation.screens.library.LibraryScreen
import com.example.questflow.presentation.screens.library.LibraryDetailScreen
import com.example.questflow.presentation.screens.library.ContactDetailScreen
import com.example.questflow.presentation.screens.library.TextTemplateLibraryScreen
import com.example.questflow.presentation.screens.timeline.TimelineScreen
import com.example.questflow.presentation.screens.settings.SettingsScreen

@Composable
fun QuestFlowNavHost(
    navController: NavHostController,
    appViewModel: AppViewModel,
    deepLinkTaskId: Long? = null
) {
    NavHost(
        navController = navController,
        startDestination = "tasks"
    ) {
        composable("tasks") {
            TasksScreen(
                appViewModel = appViewModel,
                navController = navController,
                deepLinkTaskId = deepLinkTaskId
            )
        }
        composable("collection") {
            CollectionScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("collection_manage") {
            CollectionManageScreen(
                navController = navController,
                appViewModel = appViewModel
            )
        }
        composable("skill_tree") {
            SkillTreeManagementScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("categories") {
            CategoriesScreen(
                appViewModel = appViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }
        composable("media_library") {
            MediaLibraryScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("statistics") {
            StatisticsScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("timeline") {
            TimelineScreen(
                navController = navController
            )
        }
        composable("library") {
            LibraryScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("metadata_library") {
            MetadataLibraryScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("text_templates") {
            TextTemplateLibraryScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }

        // Library detail routes
        composable("library_locations") {
            LibraryDetailScreen(
                type = "locations",
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("library_contacts") {
            LibraryDetailScreen(
                type = "contacts",
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("library_phones") {
            LibraryDetailScreen(
                type = "phones",
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("library_addresses") {
            LibraryDetailScreen(
                type = "addresses",
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("library_emails") {
            LibraryDetailScreen(
                type = "emails",
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("library_urls") {
            LibraryDetailScreen(
                type = "urls",
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("library_notes") {
            LibraryDetailScreen(
                type = "notes",
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("library_files") {
            LibraryDetailScreen(
                type = "files",
                appViewModel = appViewModel,
                navController = navController
            )
        }

        composable("tag_management") {
            com.example.questflow.presentation.screens.library.TagManagementScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = "contact_detail/{contactId}",
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
            ContactDetailScreen(
                contactId = contactId,
                appViewModel = appViewModel,
                navController = navController
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}