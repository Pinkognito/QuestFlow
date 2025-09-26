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
}