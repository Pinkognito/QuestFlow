package com.example.questflow.domain.usecase

import com.example.questflow.data.database.entity.XpSource
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.domain.usecase.category.GrantCategoryXpUseCase
import com.example.questflow.data.calendar.CalendarManager
import javax.inject.Inject

class RecordCalendarXpUseCase @Inject constructor(
    private val calendarLinkRepository: CalendarLinkRepository,
    private val taskRepository: TaskRepository,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository,
    private val grantXpUseCase: GrantXpUseCase,
    private val grantCategoryXpUseCase: GrantCategoryXpUseCase,
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase,
    private val calendarManager: CalendarManager
) {
    suspend operator fun invoke(linkId: Long): RecordCalendarXpResult {
        val link = calendarLinkRepository.getLinkById(linkId)
            ?: return RecordCalendarXpResult(success = false, message = "Calendar link not found")

        if (link.rewarded) {
            return RecordCalendarXpResult(success = false, message = "XP already claimed for this event")
        }

        // Mark as rewarded
        calendarLinkRepository.markAsRewarded(linkId)

        // Delete calendar event if deleteOnClaim flag is set
        if (link.deleteOnClaim) {
            try {
                calendarManager.deleteCalendarEvent(link.calendarEventId)
                android.util.Log.d("RecordCalendarXp", "Deleted calendar event ${link.calendarEventId} after claiming XP")
            } catch (e: Exception) {
                android.util.Log.e("RecordCalendarXp", "Failed to delete calendar event: ${e.message}")
            }
        }

        // Determine which level to use for XP calculation
        val (currentLevel, categoryName) = if (link.categoryId != null) {
            val category = categoryRepository.getCategoryById(link.categoryId)
                ?: categoryRepository.getOrCreateDefaultCategory()
            category.currentLevel to category.name
        } else {
            val currentStats = statsRepository.getOrCreateStats()
            currentStats.level to null
        }

        // Calculate XP amount based on percentage and current level
        val xpAmount = calculateXpRewardUseCase(link.xpPercentage, currentLevel)

        // Debug logging with more details
        val xpNeeded = ((currentLevel + 1) * (currentLevel + 1) * 100) - (currentLevel * currentLevel * 100)
        android.util.Log.d("RecordCalendarXp", "Level: $currentLevel, XP for next level: $xpNeeded, Percentage: ${link.xpPercentage}%, Calculated XP: $xpAmount")

        // Grant XP to category or general stats
        val (xpResult, categoryXpResult) = if (link.categoryId != null) {
            // Grant to category
            val catResult = grantCategoryXpUseCase(
                categoryId = link.categoryId,
                baseXpAmount = xpAmount,
                source = "CALENDAR",
                sourceId = link.calendarEventId
            )
            null to catResult
        } else {
            // Grant to general stats
            val genResult = grantXpUseCase(xpAmount, XpSource.CALENDAR, link.calendarEventId)
            genResult to null
        }

        // Also complete any task associated with this calendar event
        val taskWithCalendarEvent = taskRepository.getTaskByCalendarEventId(link.calendarEventId)
        taskWithCalendarEvent?.let { task ->
            if (!task.isCompleted) {
                taskRepository.toggleTaskCompletion(task.id, true)
            }
        }

        return RecordCalendarXpResult(
            success = true,
            xpGranted = xpResult?.xpGranted ?: categoryXpResult?.xpGranted,
            leveledUp = (xpResult?.levelsGained ?: categoryXpResult?.levelsGained ?: 0) > 0,
            newLevel = xpResult?.newLevel ?: categoryXpResult?.newLevel,
            unlockedMemes = xpResult?.unlockedMemes ?: emptyList(),
            categoryName = categoryName
        )
    }
}

data class RecordCalendarXpResult(
    val success: Boolean,
    val message: String? = null,
    val xpGranted: Int? = null,
    val leveledUp: Boolean = false,
    val newLevel: Int? = null,
    val unlockedMemes: List<String> = emptyList(),
    val categoryName: String? = null
)