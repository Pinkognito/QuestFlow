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
import coil.ImageLoader
import coil.Coil
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build

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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Notification permission result handled here
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Coil to support animated GIFs
        val imageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        Coil.setImageLoader(imageLoader)

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

        // Request notification permission if not already granted (Android 13+)
        requestNotificationPermissionIfNeeded()

        // Handle deep link if present
        handleDeepLink(intent)
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
                                icon = { Icon(Icons.Default.DateRange, contentDescription = "Tasks") },
                                label = { Text("Tasks") },
                                selected = currentRoute == "calendar_xp",
                                onClick = {
                                    navController.navigate("calendar_xp") {
                                        popUpTo("calendar_xp") { inclusive = true }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Star, contentDescription = "Collection") },
                                label = { Text("Collection") },
                                selected = currentRoute == "collection",
                                onClick = {
                                    navController.navigate("collection") {
                                        popUpTo("calendar_xp")
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Outlined.Info, contentDescription = "Statistiken") },
                                label = { Text("Statistik") },
                                selected = currentRoute == "statistics",
                                onClick = {
                                    navController.navigate("statistics") {
                                        popUpTo("calendar_xp")
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Build, contentDescription = "Skills") },
                                label = { Text("Skills") },
                                selected = currentRoute == "skill_tree",
                                onClick = {
                                    navController.navigate("skill_tree") {
                                        popUpTo("calendar_xp")
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Star, contentDescription = "Mediathek") },
                                label = { Text("Media") },
                                selected = currentRoute == "media_library",
                                onClick = {
                                    navController.navigate("media_library") {
                                        popUpTo("calendar_xp")
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
                                appViewModel = appViewModel,
                                deepLinkTaskId = getDeepLinkTaskId()
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private var deepLinkTaskId: Long? = null

    private fun handleDeepLink(intent: android.content.Intent) {
        val data = intent.data
        if (data != null) {
            android.util.Log.d("MainActivity", "Deep link received: $data")
            android.util.Log.d("MainActivity", "  Scheme: ${data.scheme}, Host: ${data.host}, Path: ${data.path}")

            val taskId = when {
                // Custom scheme: questflow://task/123
                data.scheme == "questflow" && data.host == "task" -> {
                    data.lastPathSegment?.toLongOrNull()
                }
                // HTTP(S) scheme: https://questflow.app/task/123
                (data.scheme == "https" || data.scheme == "http") &&
                data.host == "questflow.app" &&
                data.path?.startsWith("/task/") == true -> {
                    data.lastPathSegment?.toLongOrNull()
                }
                else -> null
            }

            if (taskId != null) {
                android.util.Log.d("MainActivity", "Extracted task ID: $taskId")
                deepLinkTaskId = taskId
            } else {
                android.util.Log.w("MainActivity", "Could not extract task ID from deep link")
            }
        }
    }

    fun getDeepLinkTaskId(): Long? {
        val taskId = deepLinkTaskId
        deepLinkTaskId = null // Clear after reading
        return taskId
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

    private fun requestNotificationPermissionIfNeeded() {
        // Notification permission is only required for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

}