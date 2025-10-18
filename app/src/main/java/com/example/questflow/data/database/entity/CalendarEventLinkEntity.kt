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
    val categoryId: Long? = null, // Associated category
    val rewarded: Boolean = false,
    val deleteOnClaim: Boolean = false, // Delete calendar event when XP is claimed
    val taskId: Long? = null, // Link to associated task for editing
    val status: String = "PENDING", // PENDING, CLAIMED, EXPIRED
    val isRecurring: Boolean = false,
    val recurringTaskId: Long? = null,
    val deleteOnExpiry: Boolean = false, // Delete calendar event when it expires
    val expiredAt: LocalDateTime? = null // FIX P1-002: Timestamp when task was marked as expired (for efficient recurring detection)
)

enum class EventStatus {
    PENDING,   // Event is waiting to be claimed
    CLAIMED,   // XP has been claimed
    EXPIRED    // Event has expired and cannot be claimed
}