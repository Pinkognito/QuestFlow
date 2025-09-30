package com.example.questflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.questflow.data.AppInitializer
import com.example.questflow.navigation.QuestFlowNavHost
import androidx.activity.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.ui.theme.QuestFlowTheme
import com.example.questflow.domain.manager.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appInitializer: AppInitializer

    @Inject
    lateinit var syncManager: SyncManager

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions result handled here
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize app data
        appInitializer.initialize()

        // Start sync manager
        lifecycleScope.launch {
            // Initial sync on app start
            syncManager.performSync(forceFullCheck = false)
            // Start periodic sync
            syncManager.startPeriodicSync()
        }

        // Request calendar permissions if not already granted
        requestCalendarPermissionsIfNeeded()
        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val selectedCategory by appViewModel.selectedCategory.collectAsState()

            // Create dynamic color scheme based on selected category
            val categoryColor = selectedCategory?.color?.let { colorHex ->
                try {
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorHex))
                } catch (e: Exception) {
                    null
                }
            }

            QuestFlowTheme(categoryColor = categoryColor) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Observe sync state
                val isSyncing by syncManager.isSyncing.collectAsState()

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Today") },
                                label = { Text("Today") },
                                selected = currentRoute == "today",
                                onClick = {
                                    navController.navigate("today") {
                                        popUpTo("today") { inclusive = true }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar XP") },
                                label = { Text("Calendar") },
                                selected = currentRoute == "calendar_xp",
                                onClick = {
                                    navController.navigate("calendar_xp") {
                                        popUpTo("today")
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Star, contentDescription = "Collection") },
                                label = { Text("Collection") },
                                selected = currentRoute == "collection",
                                onClick = {
                                    navController.navigate("collection") {
                                        popUpTo("today")
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Build, contentDescription = "Skills") },
                                label = { Text("Skills") },
                                selected = currentRoute == "skill_tree",
                                onClick = {
                                    navController.navigate("skill_tree") {
                                        popUpTo("today")
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            QuestFlowNavHost(
                                navController = navController,
                                appViewModel = appViewModel
                            )
                        }

                        // Sync indicator in bottom left corner
                        if (isSyncing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                                    .size(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncManager.stopPeriodicSync()
        syncManager.onDestroy()
    }

    private fun requestCalendarPermissionsIfNeeded() {
        val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)

        if (readPermission != PackageManager.PERMISSION_GRANTED ||
            writePermission != PackageManager.PERMISSION_GRANTED) {
            calendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
        }
    }

}