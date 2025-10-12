package com.example.questflow.data

import com.example.questflow.data.initializer.TextTemplateInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    private val textTemplateInitializer: TextTemplateInitializer
) {
    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize default text templates
            textTemplateInitializer.initializeIfNeeded()
        }
    }
}