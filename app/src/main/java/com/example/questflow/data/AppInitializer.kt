package com.example.questflow.data

import com.example.questflow.data.database.MemeDataInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    private val memeDataInitializer: MemeDataInitializer
) {
    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize memes in database
            memeDataInitializer.initializeMemes()
        }
    }
}