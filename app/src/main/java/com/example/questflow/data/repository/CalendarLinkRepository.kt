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
        categoryId: Long? = null
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
                rewarded = false
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
}