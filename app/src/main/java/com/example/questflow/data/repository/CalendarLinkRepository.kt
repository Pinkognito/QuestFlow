package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.CalendarEventLinkDao
import com.example.questflow.data.database.entity.CalendarEventLinkEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
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
        taskId: Long? = null
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
                taskId = taskId
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
}