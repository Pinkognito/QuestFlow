package com.example.questflow.domain.usecase.timeblock

import com.example.questflow.data.repository.TimeBlockRepository
import javax.inject.Inject

/**
 * Use Case: TimeBlock löschen
 */
class DeleteTimeBlockUseCase @Inject constructor(
    private val repository: TimeBlockRepository
) {
    suspend operator fun invoke(timeBlockId: Long) {
        repository.deleteTimeBlockById(timeBlockId)
    }
}
