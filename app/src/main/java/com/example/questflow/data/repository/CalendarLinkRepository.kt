package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.CalendarEventLinkDao
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CalendarLinkRepository @Inject constructor(
    private val calendarEventLinkDao: CalendarEventLinkDao
) {
    suspend fun createLink(
        calendarEventId: Long,
        title: String,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
        xp: Int,
        xpPercentage: Int = 60,
        categoryId: Long? = null,
        deleteOnClaim: Boolean = false,
        deleteOnExpiry: Boolean = false,
        taskId: Long? = null,
        isRecurring: Boolean = false,
        recurringTaskId: Long? = null  // FIX P1-002: For recurring task linking
    ): Long {
        return calendarEventLinkDao.insert(
            CalendarEventLinkEntity(
                calendarEventId = calendarEventId,
                title = title,
                startsAt = startsAt,
                endsAt = endsAt,
                xp = xp,
                xpPercentage = xpPercentage,
                categoryId = categoryId,
                rewarded = false,
                deleteOnClaim = deleteOnClaim,
                deleteOnExpiry = deleteOnExpiry,
                taskId = taskId,
                isRecurring = isRecurring,
                recurringTaskId = recurringTaskId
            )
        )
    }

    fun getUnrewardedLinks(): Flow<List<CalendarEventLinkEntity>> {
        return calendarEventLinkDao.getUnrewardedLinks()
    }

    fun getAllLinks(): Flow<List<CalendarEventLinkEntity>> {
        return calendarEventLinkDao.getAllLinks()
    }

    suspend fun markAsRewarded(linkId: Long) {
        calendarEventLinkDao.markAsRewarded(linkId)
    }

    suspend fun getLinkById(linkId: Long): CalendarEventLinkEntity? {
        return calendarEventLinkDao.getLinkById(linkId)
    }

    suspend fun insertLink(link: CalendarEventLinkEntity): Long {
        return calendarEventLinkDao.insert(link)
    }

    suspend fun updateLink(link: CalendarEventLinkEntity) {
        calendarEventLinkDao.update(link)
    }

    suspend fun updateLink(
        linkId: Long,
        title: String,
        description: String? = null,
        xpPercentage: Int,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
        categoryId: Long?
    ) {
        val existingLink = calendarEventLinkDao.getLinkById(linkId) ?: return
        val updatedLink = existingLink.copy(
            title = title,
            xpPercentage = xpPercentage,
            startsAt = startsAt,
            endsAt = endsAt,
            categoryId = categoryId
        )
        calendarEventLinkDao.update(updatedLink)
    }

    suspend fun unclaimByTaskId(taskId: Long) {
        calendarEventLinkDao.unclaimByTaskId(taskId)
    }

    suspend fun updateLinkStatus(linkId: Long, status: String) {
        val link = calendarEventLinkDao.getLinkById(linkId)
        link?.let {
            calendarEventLinkDao.update(it.copy(status = status))
        }
    }

    suspend fun getLinkByCalendarEventId(calendarEventId: Long): CalendarEventLinkEntity? {
        return calendarEventLinkDao.getLinkByCalendarEventId(calendarEventId)
    }

    /**
     * Get all calendar events in a date range for occupancy visualization
     * @param startDate First day of range
     * @param endDate Last day of range (exclusive)
     */
    fun getEventsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<CalendarEventLinkEntity>> {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val startString = startDate.atStartOfDay().format(formatter)
        val endString = endDate.atStartOfDay().format(formatter)
        return calendarEventLinkDao.getEventsInRange(startString, endString)
    }

    /**
     * Get all calendar event links associated with a task
     * ZOMBIE FIX: Needed to clean up CalendarEventLinks when task is deleted
     */
    suspend fun getLinksByTaskId(taskId: Long): List<CalendarEventLinkEntity> {
        return calendarEventLinkDao.getLinksByTaskId(taskId)
    }

    /**
     * Delete all calendar event links associated with a task
     * ZOMBIE FIX: Prevents orphaned CalendarEventLinks in database
     */
    suspend fun deleteByTaskId(taskId: Long) {
        calendarEventLinkDao.deleteByTaskId(taskId)
    }

    /**
     * Delete all orphaned calendar event links (taskId points to deleted task)
     * CLEANUP: One-time operation to remove zombie events
     * @return Number of deleted links
     */
    suspend fun deleteOrphanedLinks(): Int {
        return calendarEventLinkDao.deleteOrphanedLinks()
    }
}
