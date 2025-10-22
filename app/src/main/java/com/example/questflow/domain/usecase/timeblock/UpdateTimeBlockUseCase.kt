package com.example.questflow.domain.usecase.timeblock

import com.example.questflow.data.database.entity.TimeBlockEntity
import com.example.questflow.data.repository.TimeBlockRepository
import javax.inject.Inject

/**
 * Use Case: TimeBlock aktualisieren
 */
class UpdateTimeBlockUseCase @Inject constructor(
    private val repository: TimeBlockRepository
) {
    suspend operator fun invoke(
        timeBlock: TimeBlockEntity,
        tagIds: List<Long> = emptyList()
    ): Long {
        return repository.saveTimeBlockWithTags(timeBlock, tagIds)
    }
}
