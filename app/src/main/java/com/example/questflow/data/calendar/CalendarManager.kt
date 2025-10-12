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
import java.time.LocalDate
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
        categoryColor: String? = null,
        taskId: Long? = null
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext null

        val calendarId = getOrCreateCalendar() ?: return@withContext null

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            put(CalendarContract.Events.DTEND, endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            put(CalendarContract.Events.TITLE, taskTitle) // Title already includes emoji from ViewModel

            // Build description with taskId for reference
            val descriptionText = buildString {
                append(taskDescription)
                append("\n\n🎮 XP Reward: $xpReward")
                if (taskId != null) {
                    // Include taskId as reference - app will handle opening from notification
                    append("\n\n📱 Öffne QuestFlow, um den Task zu bearbeiten")
                    append("\n(Task ID: $taskId)")
                }
            }
            put(CalendarContract.Events.DESCRIPTION, descriptionText)

            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            // Disable Google Calendar alarms - we use our custom notification system
            put(CalendarContract.Events.HAS_ALARM, 0)
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

        // NOTE: Google Calendar reminder disabled - we use our own custom notification system
        // with deep link button via TaskNotificationManager and WorkManager
        // This prevents duplicate notifications (Google's standard + our custom)

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
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_ID
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
                val calId = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))

                events.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        description = description,
                        startTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(startMillis),
                            ZoneId.systemDefault()
                        ),
                        endTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(endMillis),
                            ZoneId.systemDefault()
                        ),
                        calendarId = calId,
                        isExternal = false
                    )
                )
            }
        }

        events
    }

    /**
     * Get ALL calendar events from ALL calendars on the device (Google Calendar, Outlook, etc.)
     * for Timeline display. These are read-only.
     */
    suspend fun getAllCalendarEvents(startDate: LocalDate, endDate: LocalDate): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext emptyList()

        val events = mutableListOf<CalendarEvent>()
        val questFlowCalendarId = getCalendarId()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )

        // Filter by date range
        val startMillis = startDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val selection = "(${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DTEND} >= ?)"
        val selectionArgs = arrayOf(endMillis.toString(), startMillis.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events._ID))
                val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: ""
                val description = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)) ?: ""
                val startMillis = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                val endMillis = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                val calId = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))
                val calendarName = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)) ?: ""

                // Mark as external if not from QuestFlow calendar
                val isExternal = questFlowCalendarId != calId

                events.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        description = description,
                        startTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(startMillis),
                            ZoneId.systemDefault()
                        ),
                        endTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(endMillis),
                            ZoneId.systemDefault()
                        ),
                        calendarId = calId,
                        calendarName = calendarName,
                        isExternal = isExternal
                    )
                )
            }
        }

        events
    }

    suspend fun getCalendarEvent(eventId: Long): CalendarEvent? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext null

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        var event: CalendarEvent? = null
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events._ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: ""
                val description = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)) ?: ""
                val startMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                val endMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))

                event = CalendarEvent(
                    id = id,
                    title = title,
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
            }
        }
        event
    }

    suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext false
        }

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.delete(uri, null, null)
        rows > 0
    }

    suspend fun updateTaskEvent(
        eventId: Long,
        taskTitle: String,
        taskDescription: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime = startTime.plusHours(1)
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext false
        }

        // Check if event exists first
        val eventExists = try {
            getCalendarEvent(eventId) != null
        } catch (e: Exception) {
            android.util.Log.e("CalendarManager", "Event check failed: ${e.message}")
            false
        }

        if (!eventExists) {
            android.util.Log.e("CalendarManager", "Event $eventId does not exist - cannot update")
            return@withContext false
        }

        val startMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, taskTitle)
            put(CalendarContract.Events.DESCRIPTION, taskDescription)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
        }

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.update(uri, values, null, null)
        rows > 0
    }
}

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val calendarId: Long = 0,
    val calendarName: String = "",
    val isExternal: Boolean = false // true if from Google Calendar, Outlook, etc.
)