package com.example.questflow

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuestFlowApplication : Application(), Configuration.Provider {
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        // WorkManager is auto-initialized via Configuration.Provider
    }
}