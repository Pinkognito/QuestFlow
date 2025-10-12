package com.example.questflow.presentation.screens.timeline.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Utility object for timeline calculations including:
 * - Time to pixel conversions
 * - Pixel to time conversions
 * - Task positioning and sizing
 * - Grid snapping
 */
object TimelineCalculator {

    /**
     * Convert time to horizontal pixel position.
     *
     * @param time Time to convert
     * @param dayStart Start hour of the visible day range (e.g., 6 for 6:00 AM)
     * @param pixelsPerMinute Zoom level (pixels per minute)
     * @return Horizontal offset in pixels from the start of the day
     */
    fun timeToPixel(time: LocalTime, dayStart: Int, pixelsPerMinute: Float): Float {
        val minutesSinceDayStart = (time.hour * 60 + time.minute) - (dayStart * 60)
        return minutesSinceDayStart * pixelsPerMinute
    }

    /**
     * Convert LocalDateTime to pixel position
     */
    fun dateTimeToPixel(dateTime: LocalDateTime, dayStart: Int, pixelsPerMinute: Float): Float {
        return timeToPixel(dateTime.toLocalTime(), dayStart, pixelsPerMinute)
    }

    /**
     * Convert horizontal pixel position to time.
     *
     * @param pixel Horizontal offset in pixels
     * @param dayStart Start hour of the visible day range
     * @param pixelsPerMinute Zoom level (pixels per minute)
     * @return LocalTime corresponding to the pixel position
     */
    fun pixelToTime(pixel: Float, dayStart: Int, pixelsPerMinute: Float): LocalTime {
        val totalMinutes = (pixel / pixelsPerMinute).toInt() + (dayStart * 60)
        val hours = (totalMinutes / 60).coerceIn(0, 23)
        val minutes = (totalMinutes % 60).coerceIn(0, 59)
        return LocalTime.of(hours, minutes)
    }

    /**
     * Convert pixel offset to LocalDateTime for a specific date
     */
    fun pixelToDateTime(pixel: Float, date: LocalDate, dayStart: Int, pixelsPerMinute: Float): LocalDateTime {
        val time = pixelToTime(pixel, dayStart, pixelsPerMinute)
        return date.atTime(time)
    }

    /**
     * Calculate task width in pixels based on duration.
     *
     * @param startTime Task start time
     * @param endTime Task end time
     * @param pixelsPerMinute Zoom level
     * @return Width in pixels
     */
    fun calculateTaskWidth(startTime: LocalDateTime, endTime: LocalDateTime, pixelsPerMinute: Float): Float {
        val durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime)
        return durationMinutes * pixelsPerMinute
    }

    /**
     * Calculate the total width of the timeline in pixels.
     *
     * @param hourStart Start hour (e.g., 6 for 6 AM)
     * @param hourEnd End hour (e.g., 22 for 10 PM)
     * @param pixelsPerMinute Zoom level
     * @return Total width in pixels
     */
    fun calculateTimelineWidth(hourStart: Int, hourEnd: Int, pixelsPerMinute: Float): Float {
        val totalHours = hourEnd - hourStart
        return totalHours * 60 * pixelsPerMinute
    }

    /**
     * Snap time to the nearest grid interval.
     *
     * @param time Time to snap
     * @param gridMinutes Grid interval in minutes (e.g., 15)
     * @return Snapped time
     */
    fun snapToGrid(time: LocalTime, gridMinutes: Int): LocalTime {
        val totalMinutes = time.hour * 60 + time.minute
        val snappedMinutes = ((totalMinutes + gridMinutes / 2) / gridMinutes) * gridMinutes
        val hours = (snappedMinutes / 60).coerceIn(0, 23)
        val minutes = (snappedMinutes % 60).coerceIn(0, 59)
        return LocalTime.of(hours, minutes)
    }

    /**
     * Snap LocalDateTime to grid
     */
    fun snapDateTimeToGrid(dateTime: LocalDateTime, gridMinutes: Int): LocalDateTime {
        val snappedTime = snapToGrid(dateTime.toLocalTime(), gridMinutes)
        return dateTime.toLocalDate().atTime(snappedTime)
    }

    /**
     * Calculate time offset from drag delta.
     *
     * @param dragDeltaX Horizontal drag distance in pixels
     * @param pixelsPerMinute Zoom level
     * @param gridMinutes Snap-to-grid interval
     * @return Time offset in minutes (snapped to grid)
     */
    fun calculateTimeOffsetFromDrag(dragDeltaX: Float, pixelsPerMinute: Float, gridMinutes: Int): Int {
        val rawMinutes = (dragDeltaX / pixelsPerMinute).toInt()
        return ((rawMinutes + gridMinutes / 2) / gridMinutes) * gridMinutes
    }

    /**
     * Calculate new task times after drag.
     *
     * @param originalStart Original start time
     * @param originalEnd Original end time
     * @param dragDeltaX Horizontal drag distance in pixels
     * @param pixelsPerMinute Zoom level
     * @param gridMinutes Snap-to-grid interval
     * @return Pair of (new start time, new end time)
     */
    fun calculateDraggedTaskTimes(
        originalStart: LocalDateTime,
        originalEnd: LocalDateTime,
        dragDeltaX: Float,
        pixelsPerMinute: Float,
        gridMinutes: Int
    ): Pair<LocalDateTime, LocalDateTime> {
        val offsetMinutes = calculateTimeOffsetFromDrag(dragDeltaX, pixelsPerMinute, gridMinutes)

        val newStart = originalStart.plusMinutes(offsetMinutes.toLong())
        val newEnd = originalEnd.plusMinutes(offsetMinutes.toLong())

        return newStart to newEnd
    }

    /**
     * Check if time is within visible range.
     *
     * @param time Time to check
     * @param hourStart Start of visible range
     * @param hourEnd End of visible range
     * @return True if time is visible
     */
    fun isTimeInVisibleRange(time: LocalTime, hourStart: Int, hourEnd: Int): Boolean {
        val hour = time.hour
        return hour in hourStart until hourEnd
    }

    /**
     * Clamp time to visible range.
     *
     * @param time Time to clamp
     * @param hourStart Start of visible range
     * @param hourEnd End of visible range
     * @return Clamped time
     */
    fun clampToVisibleRange(time: LocalTime, hourStart: Int, hourEnd: Int): LocalTime {
        return when {
            time.hour < hourStart -> LocalTime.of(hourStart, 0)
            time.hour >= hourEnd -> LocalTime.of(hourEnd - 1, 59)
            else -> time
        }
    }

    /**
     * Format time for display in timeline.
     *
     * @param time Time to format
     * @param showMinutes Whether to show minutes
     * @return Formatted string (e.g., "14:00" or "14h")
     */
    fun formatTimeForDisplay(time: LocalTime, showMinutes: Boolean = true): String {
        return if (showMinutes) {
            String.format("%02d:%02d", time.hour, time.minute)
        } else {
            "${time.hour}h"
        }
    }

    /**
     * Calculate overlap percentage between two time ranges.
     *
     * @return Overlap percentage (0.0 to 1.0)
     */
    fun calculateOverlapPercentage(
        start1: LocalDateTime,
        end1: LocalDateTime,
        start2: LocalDateTime,
        end2: LocalDateTime
    ): Float {
        if (end1 <= start2 || start1 >= end2) return 0f

        val overlapStart = maxOf(start1, start2)
        val overlapEnd = minOf(end1, end2)
        val overlapMinutes = ChronoUnit.MINUTES.between(overlapStart, overlapEnd)

        val duration1 = ChronoUnit.MINUTES.between(start1, end1)
        val duration2 = ChronoUnit.MINUTES.between(start2, end2)
        val avgDuration = (duration1 + duration2) / 2

        return (overlapMinutes.toFloat() / avgDuration).coerceIn(0f, 1f)
    }
}
