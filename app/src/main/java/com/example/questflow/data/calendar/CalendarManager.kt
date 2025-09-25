package com.example.questflow.data.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CALENDAR_DISPLAY_NAME = "QuestFlow Tasks"
        private const val CALENDAR_ACCOUNT_NAME = "QuestFlow"
        private const val CALENDAR_ACCOUNT_TYPE = "com.example.questflow"
    }

    suspend fun hasCalendarPermission(): Boolean = withContext(Dispatchers.IO) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getOrCreateCalendar(): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext null

        val calendarId = getCalendarId()
        if (calendarId != null) return@withContext calendarId

        createCalendar()
    }

    private fun getCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND " +
                       "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(CALENDAR_ACCOUNT_NAME, CALENDAR_ACCOUNT_TYPE)

        val cursor: Cursor? = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            }
        }
        return null
    }

    private fun createCalendar(): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
            put(CalendarContract.Calendars.NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF2196F3.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }

        val uri: Uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
            .build()

        val insertedUri = context.contentResolver.insert(uri, values)
        return insertedUri?.let { ContentUris.parseId(it) }
    }

    suspend fun createTaskEvent(
        taskTitle: String,
        taskDescription: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        xpReward: Int,
        xpPercentage: Int = 60,
        categoryColor: String? = null
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext null

        val calendarId = getOrCreateCalendar() ?: return@withContext null

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            put(CalendarContract.Events.DTEND, endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            put(CalendarContract.Events.TITLE, taskTitle) // Title already includes emoji from ViewModel
            put(CalendarContract.Events.DESCRIPTION, "$taskDescription\n\n🎮 XP Reward: $xpReward")
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
            // Set event color if category color is provided
            categoryColor?.let {
                try {
                    val color = android.graphics.Color.parseColor(it)
                    put(CalendarContract.Events.EVENT_COLOR, color)
                } catch (e: Exception) {
                    // Invalid color format, ignore
                }
            }
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val eventId = uri?.let { ContentUris.parseId(it) }

        // Add a reminder 15 minutes before
        eventId?.let {
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 15)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        }

        eventId
    }

    suspend fun getCalendarEvents(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext emptyList()

        val calendarId = getCalendarId() ?: return@withContext emptyList()
        val events = mutableListOf<CalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events._ID))
                val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: ""
                val description = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)) ?: ""
                val startMillis = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                val endMillis = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTEND))

                events.add(
                    CalendarEvent(
                        id = id,
                        title = title, // Keep emoji prefix from category
                        description = description,
                        startTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(startMillis),
                            ZoneId.systemDefault()
                        ),
                        endTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(endMillis),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
        }

        events
    }

    suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext false

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.delete(uri, null, null)
        rows > 0
    }
}

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
)