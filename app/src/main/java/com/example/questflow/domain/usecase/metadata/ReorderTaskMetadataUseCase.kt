package com.example.questflow.domain.usecase.metadata

import android.util.Log
import com.example.questflow.data.database.dao.TaskMetadataDao
import com.example.questflow.domain.model.TaskMetadataItem
import javax.inject.Inject

/**
 * Reorders metadata items for a task by updating their display order.
 */
class ReorderTaskMetadataUseCase @Inject constructor(
    private val taskMetadataDao: TaskMetadataDao
) {
    suspend operator fun invoke(items: List<TaskMetadataItem>): Result<Unit> = runCatching {
        Log.d(TAG, "Reordering ${items.size} metadata items")

        items.forEachIndexed { index, item ->
            Log.d(TAG, "Setting displayOrder=$index for metadata ${item.metadataId}")
            taskMetadataDao.updateDisplayOrder(item.metadataId, index)
        }

        Log.d(TAG, "Successfully reordered metadata items")
        Unit
    }.onFailure { exception ->
        Log.e(TAG, "Failed to reorder metadata items", exception)
    }

    companion object {
        private const val TAG = "ReorderTaskMetadata"
    }
}
