package com.example.questflow.data

import com.example.questflow.data.database.MemeDataInitializer
import com.example.questflow.data.initializer.TextTemplateInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    private val memeDataInitializer: MemeDataInitializer,
    private val textTemplateInitializer: TextTemplateInitializer
) {
    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize memes in database
            memeDataInitializer.initializeMemes()

            // Initialize default text templates
            textTemplateInitializer.initializeIfNeeded()
        }
    }
}