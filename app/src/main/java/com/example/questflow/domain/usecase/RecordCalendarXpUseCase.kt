package com.example.questflow.domain.usecase

import com.example.questflow.data.database.entity.XpSource
import com.example.questflow.data.repository.CalendarLinkRepository
import com.example.questflow.data.repository.TaskRepository
import com.example.questflow.data.repository.StatsRepository
import com.example.questflow.data.repository.CategoryRepository
import com.example.questflow.domain.usecase.category.GrantCategoryXpUseCase
import com.example.questflow.data.calendar.CalendarManager
import com.example.questflow.data.database.dao.TaskHistoryDao
import com.example.questflow.data.database.entity.TaskHistoryEntity
import java.time.LocalDateTime
import javax.inject.Inject

class RecordCalendarXpUseCase @Inject constructor(
    private val calendarLinkRepository: CalendarLinkRepository,
    private val taskRepository: TaskRepository,
    private val statsRepository: StatsRepository,
    private val categoryRepository: CategoryRepository,
    private val grantXpUseCase: GrantXpUseCase,
    private val grantCategoryXpUseCase: GrantCategoryXpUseCase,
    private val taskHistoryDao: TaskHistoryDao,  // Task History System
    private val calculateXpRewardUseCase: CalculateXpRewardUseCase,
    private val calendarManager: CalendarManager,
    private val completeTaskUseCase: CompleteTaskUseCase
) {
    suspend operator fun invoke(linkId: Long): RecordCalendarXpResult {
        android.util.Log.d("QuestFlow_XpClaim", "========================================")
        android.util.Log.d("QuestFlow_XpClaim", "=== XP CLAIM STARTED ===")
        android.util.Log.d("QuestFlow_XpClaim", "Link ID: $linkId")

        val link = calendarLinkRepository.getLinkById(linkId)
            ?: return RecordCalendarXpResult(success = false, message = "Calendar link not found")

        android.util.Log.d("QuestFlow_XpClaim", "Link found: title='${link.title}', calendarEventId=${link.calendarEventId}")
        android.util.Log.d("QuestFlow_XpClaim", "Link taskId: ${link.taskId}")
        android.util.Log.d("QuestFlow_XpClaim", "Link deleteOnClaim: ${link.deleteOnClaim}")
        android.util.Log.d("QuestFlow_XpClaim", "Link deleteOnExpiry: ${link.deleteOnExpiry}")
        android.util.Log.d("QuestFlow_XpClaim", "Link rewarded: ${link.rewarded}")
        android.util.Log.d("QuestFlow_XpClaim", "Link status: ${link.status}")

        if (link.rewarded) {
            android.util.Log.w("QuestFlow_XpClaim", "XP already claimed - aborting")
            return RecordCalendarXpResult(success = false, message = "XP already claimed for this event")
        }

        // Allow claiming even if expired
        // This enables users to still get XP from expired events

        // Mark as rewarded and update status to CLAIMED
        taskHistoryDao.insert(
            TaskHistoryEntity(
                taskId = link.taskId ?: 0,
                eventType = "CLAIMED",
                timestamp = LocalDateTime.now()
            )
        )
        android.util.Log.d("QuestFlow_XpClaim", "Marking link as CLAIMED (rewarded=true)")
        calendarLinkRepository.updateLink(
            link.copy(rewarded = true, status = "CLAIMED")
        )

        // Delete calendar event if deleteOnClaim flag is set
        if (link.deleteOnClaim) {
            android.util.Log.d("QuestFlow_XpClaim", "deleteOnClaim=true → Deleting CALENDAR EVENT (NOT TASK!)")
            try {
                calendarManager.deleteEvent(link.calendarEventId)
                android.util.Log.d("QuestFlow_XpClaim", "✅ Successfully deleted calendar event ${link.calendarEventId}")
            } catch (e: Exception) {
                android.util.Log.e("QuestFlow_XpClaim", "❌ Failed to delete calendar event: ${e.message}")
            }
        } else {
            android.util.Log.d("QuestFlow_XpClaim", "deleteOnClaim=false → Keeping calendar event")
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
        android.util.Log.d("QuestFlow_XpClaim", "Level: $currentLevel, XP for next level: $xpNeeded, Percentage: ${link.xpPercentage}%, Calculated XP: $xpAmount")

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

        android.util.Log.d("QuestFlow_XpClaim", "XP granted: ${xpResult?.xpGranted ?: categoryXpResult?.xpGranted}")

        // Also complete any task associated with this calendar event
        android.util.Log.d("QuestFlow_XpClaim", "Checking for associated task with calendarEventId=${link.calendarEventId}")
        val taskWithCalendarEvent = taskRepository.getTaskByCalendarEventId(link.calendarEventId)
        if (taskWithCalendarEvent != null) {
            android.util.Log.d("QuestFlow_XpClaim", "Task found: id=${taskWithCalendarEvent.id}, title='${taskWithCalendarEvent.title}'")
            android.util.Log.d("QuestFlow_XpClaim", "Task completed: ${taskWithCalendarEvent.isCompleted}")
            if (!taskWithCalendarEvent.isCompleted) {
                android.util.Log.d("QuestFlow_XpClaim", "Calling completeTaskUseCase for task ${taskWithCalendarEvent.id}")
                completeTaskUseCase(taskWithCalendarEvent.id)
                android.util.Log.d("QuestFlow_XpClaim", "completeTaskUseCase completed for task ${taskWithCalendarEvent.id}")
            } else {
                android.util.Log.d("QuestFlow_XpClaim", "Task already completed, skipping")
            }
        } else {
            android.util.Log.d("QuestFlow_XpClaim", "No task found with calendarEventId=${link.calendarEventId}")
        }

        android.util.Log.d("QuestFlow_XpClaim", "=== XP CLAIM FINISHED ===")
        android.util.Log.d("QuestFlow_XpClaim", "========================================")

        return RecordCalendarXpResult(
            success = true,
            xpGranted = xpResult?.xpGranted ?: categoryXpResult?.xpGranted,
            leveledUp = (xpResult?.levelsGained ?: categoryXpResult?.levelsGained ?: 0) > 0,
            newLevel = xpResult?.newLevel ?: categoryXpResult?.newLevel,
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
    val categoryName: String? = null
)
