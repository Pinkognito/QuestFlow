package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventLinkDao {
    @Insert
    suspend fun insert(link: CalendarEventLinkEntity): Long

    @Query("SELECT * FROM calendar_event_links WHERE rewarded = 0 ORDER BY startsAt DESC")
    fun getUnrewardedLinks(): Flow<List<CalendarEventLinkEntity>>

    @Query("SELECT * FROM calendar_event_links ORDER BY startsAt DESC")
    fun getAllLinks(): Flow<List<CalendarEventLinkEntity>>

    @Query("UPDATE calendar_event_links SET rewarded = 1 WHERE id = :linkId")
    suspend fun markAsRewarded(linkId: Long)

    @Query("SELECT * FROM calendar_event_links WHERE id = :linkId")
    suspend fun getLinkById(linkId: Long): CalendarEventLinkEntity?

    @Update
    suspend fun update(link: CalendarEventLinkEntity)

    @Query("UPDATE calendar_event_links SET rewarded = 0, status = 'PENDING' WHERE taskId = :taskId")
    suspend fun unclaimByTaskId(taskId: Long)

    @Query("SELECT * FROM calendar_event_links WHERE calendarEventId = :calendarEventId")
    suspend fun getLinkByCalendarEventId(calendarEventId: Long): CalendarEventLinkEntity?

    /**
     * Get all calendar event links associated with a task
     */
    @Query("SELECT * FROM calendar_event_links WHERE taskId = :taskId")
    suspend fun getLinksByTaskId(taskId: Long): List<CalendarEventLinkEntity>

    /**
     * Delete all calendar event links associated with a task
     */
    @Query("DELETE FROM calendar_event_links WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)

    /**
     * Delete a single calendar event link
     */
    @Delete
    suspend fun delete(link: CalendarEventLinkEntity)

    /**
     * Get all calendar events in a date range for occupancy visualization
     * DEFENSIVE: Filters out orphaned events (taskId points to deleted task)
     * @param startDate ISO format string (e.g., "2025-10-01T00:00:00")
     * @param endDate ISO format string (e.g., "2025-11-01T00:00:00")
     */
    @Query("""
        SELECT * FROM calendar_event_links
        WHERE startsAt >= :startDate
        AND startsAt < :endDate
        AND (taskId IS NULL OR taskId IN (SELECT id FROM tasks))
        ORDER BY startsAt ASC
    """)
    fun getEventsInRange(startDate: String, endDate: String): Flow<List<CalendarEventLinkEntity>>

    /**
     * Delete all orphaned calendar event links (taskId points to deleted task)
     * CLEANUP: One-time operation to remove zombie events
     * @return Number of deleted links
     */
    @Query("""
        DELETE FROM calendar_event_links
        WHERE taskId IS NOT NULL
        AND taskId NOT IN (SELECT id FROM tasks)
    """)
    suspend fun deleteOrphanedLinks(): Int
}