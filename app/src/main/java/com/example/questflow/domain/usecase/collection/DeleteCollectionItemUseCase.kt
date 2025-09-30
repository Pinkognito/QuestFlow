package com.example.questflow.domain.usecase.collection

import android.util.Log
import com.example.questflow.data.database.entity.CollectionItemEntity
import com.example.questflow.data.repository.CollectionRepository
import javax.inject.Inject

class DeleteCollectionItemUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {
    companion object {
        private const val TAG = "DeleteCollectionItemUseCase"
    }

    suspend operator fun invoke(item: CollectionItemEntity) {
        Log.d(TAG, "Deleting collection item: ${item.id} - ${item.name}")
        collectionRepository.deleteCollectionItem(item)
        Log.d(TAG, "Collection item deleted successfully")
    }
}
