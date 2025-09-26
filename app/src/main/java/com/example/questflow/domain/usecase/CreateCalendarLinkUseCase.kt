package com.example.questflow.domain.usecase

import com.example.questflow.data.repository.CalendarLinkRepository
import java.time.LocalDateTime
import javax.inject.Inject

class CreateCalendarLinkUseCase @Inject constructor(
    private val calendarLinkRepository: CalendarLinkRepository
) {
    suspend operator fun invoke(
        calendarEventId: Long,
        title: String,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
        xp: Int,
        xpPercentage: Int = 60,
        categoryId: Long? = null,
        deleteOnClaim: Boolean = false,
        taskId: Long? = null
    ): Long {
        return calendarLinkRepository.createLink(
            calendarEventId = calendarEventId,
            title = title,
            startsAt = startsAt,
            endsAt = endsAt,
            xp = xp,
            xpPercentage = xpPercentage,
            categoryId = categoryId,
            deleteOnClaim = deleteOnClaim,
            taskId = taskId
        )
    }
}