package com.example.questflow.domain.usecase.timeblock

import com.example.questflow.data.database.dao.TimeBlockWithTags
import com.example.questflow.data.repository.TimeBlockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case: Aktive TimeBlocks abrufen
 */
class GetActiveTimeBlocksUseCase @Inject constructor(
    private val repository: TimeBlockRepository
) {
    operator fun invoke(): Flow<List<TimeBlockWithTags>> {
        return repository.getActiveTimeBlocksWithTagsFlow()
    }
}
