package com.example.questflow

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.questflow.data.debug.DebugDataInitializer
import com.example.questflow.data.repository.TagRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class QuestFlowApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var tagRepository: TagRepository

    @Inject
    lateinit var debugDataInitializer: DebugDataInitializer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // WorkManager is auto-initialized via Configuration.Provider

        // Initialize standard tags if database is empty
        applicationScope.launch {
            tagRepository.initializeStandardTagsIfEmpty()
        }

        // Initialize debug data if database is empty
        applicationScope.launch {
            if (debugDataInitializer.shouldInitialize()) {
                android.util.Log.d("QuestFlowApp", "🐛 Initializing debug test data...")
                debugDataInitializer.initialize()
                android.util.Log.d("QuestFlowApp", "✅ Debug test data initialized!")
            }
        }
    }
}