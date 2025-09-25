package com.example.questflow.domain.usecase

import com.example.questflow.data.database.entity.XpSource
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.TaskRepository
import javax.inject.Inject

class RecordCalendarXpUseCase @Inject constructor(
    private val calendarLinkRepository: CalendarLinkRepository,
    private val taskRepository: TaskRepository,
    private val grantXpUseCase: GrantXpUseCase
) {
    suspend operator fun invoke(linkId: Long): RecordCalendarXpResult {
        val link = calendarLinkRepository.getLinkById(linkId)
            ?: return RecordCalendarXpResult(success = false, message = "Calendar link not found")

        if (link.rewarded) {
            return RecordCalendarXpResult(success = false, message = "XP already claimed for this event")
        }

        // Mark as rewarded
        calendarLinkRepository.markAsRewarded(linkId)

        // Grant XP
        val xpResult = grantXpUseCase(link.xp, XpSource.CALENDAR, link.calendarEventId)

        // Also complete any task associated with this calendar event
        val taskWithCalendarEvent = taskRepository.getTaskByCalendarEventId(link.calendarEventId)
        taskWithCalendarEvent?.let { task ->
            if (!task.isCompleted) {
                taskRepository.toggleTaskCompletion(task.id, true)
            }
        }

        return RecordCalendarXpResult(
            success = true,
            xpGranted = xpResult.xpGranted,
            leveledUp = xpResult.levelsGained > 0,
            newLevel = xpResult.newLevel,
            unlockedMemes = xpResult.unlockedMemes
        )
    }
}

data class RecordCalendarXpResult(
    val success: Boolean,
    val message: String? = null,
    val xpGranted: Int? = null,
    val leveledUp: Boolean = false,
    val newLevel: Int? = null,
    val unlockedMemes: List<String> = emptyList()
)