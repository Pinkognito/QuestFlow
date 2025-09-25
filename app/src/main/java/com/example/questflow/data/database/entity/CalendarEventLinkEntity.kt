package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "calendar_event_links")
data class CalendarEventLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val calendarEventId: Long,
    val title: String,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val xp: Int,
    val xpPercentage: Int = 60, // Default to medium difficulty
    val rewarded: Boolean = false
)