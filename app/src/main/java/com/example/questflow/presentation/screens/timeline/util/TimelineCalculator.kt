package com.example.questflow.presentation.screens.timeline.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Utility object for timeline calculations.
 *
 * ARCHITECTURE: Time axis is VERTICAL (Y-axis), days are HORIZONTAL (X-axis)
 * - 00:00 is at Y=0 (top)
 * - 23:59 is at Y=max (bottom)
 * - Days scroll horizontally
 * - Time scrolls vertically (synchronized across all day columns)
 */
object TimelineCalculator {

    /**
     * Convert time to vertical pixel position (Y-axis).
     *
     * @param time Time to convert
     * @param pixelsPerMinute Zoom level (pixels per minute)
     * @return Vertical offset in pixels from midnight (00:00)
     */
    fun timeToPixel(time: LocalTime, pixelsPerMinute: Float): Float {
        val minutesSinceMidnight = time.hour * 60 + time.minute
        return minutesSinceMidnight * pixelsPerMinute
    }

    /**
     * Convert LocalDateTime to vertical pixel position.
     */
    fun dateTimeToPixel(dateTime: LocalDateTime, pixelsPerMinute: Float): Float {
        return timeToPixel(dateTime.toLocalTime(), pixelsPerMinute)
    }

    /**
     * Convert vertical pixel position to time.
     *
     * @param pixelY Vertical offset in pixels from midnight
     * @param pixelsPerMinute Zoom level (pixels per minute)
     * @return LocalTime corresponding to the pixel position
     */
    fun pixelToTime(pixelY: Float, pixelsPerMinute: Float): LocalTime {
        val totalMinutes = (pixelY / pixelsPerMinute).toInt().coerceIn(0, 24 * 60 - 1)
        val hours = (totalMinutes / 60).coerceIn(0, 23)
        val minutes = (totalMinutes % 60).coerceIn(0, 59)
        return LocalTime.of(hours, minutes)
    }

    /**
     * Convert pixel offset to LocalDateTime for a specific date.
     */
    fun pixelToDateTime(pixelY: Float, date: LocalDate, pixelsPerMinute: Float): LocalDateTime {
        val time = pixelToTime(pixelY, pixelsPerMinute)
        return date.atTime(time)
    }

    /**
     * Calculate task HEIGHT in pixels based on duration.
     * (Previously was width, now height because time is vertical)
     *
     * @param startTime Task start time
     * @param endTime Task end time
     * @param pixelsPerMinute Zoom level
     * @return Height in pixels
     */
    fun calculateTaskHeight(startTime: LocalDateTime, endTime: LocalDateTime, pixelsPerMinute: Float): Float {
        val durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime).coerceAtLeast(1)
        return durationMinutes * pixelsPerMinute
    }

    /**
     * Calculate the total HEIGHT of the timeline in pixels (full 24 hours).
     *
     * @param pixelsPerMinute Zoom level
     * @return Total height in pixels for 24 hours
     */
    fun calculateTimelineHeight(pixelsPerMinute: Float): Float {
        return 24 * 60 * pixelsPerMinute // 24 hours * 60 minutes * pixels per minute
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
     * Snap LocalDateTime to grid.
     */
    fun snapDateTimeToGrid(dateTime: LocalDateTime, gridMinutes: Int): LocalDateTime {
        val snappedTime = snapToGrid(dateTime.toLocalTime(), gridMinutes)
        return dateTime.toLocalDate().atTime(snappedTime)
    }

    /**
     * Calculate time offset from drag delta (VERTICAL drag now).
     *
     * @param dragDeltaY Vertical drag distance in pixels
     * @param pixelsPerMinute Zoom level
     * @param gridMinutes Snap-to-grid interval
     * @return Time offset in minutes (snapped to grid)
     */
    fun calculateTimeOffsetFromDrag(dragDeltaY: Float, pixelsPerMinute: Float, gridMinutes: Int): Int {
        val rawMinutes = (dragDeltaY / pixelsPerMinute).toInt()
        return ((rawMinutes + gridMinutes / 2) / gridMinutes) * gridMinutes
    }

    /**
     * Calculate new task times after vertical drag.
     *
     * @param originalStart Original start time
     * @param originalEnd Original end time
     * @param dragDeltaY Vertical drag distance in pixels
     * @param pixelsPerMinute Zoom level
     * @param gridMinutes Snap-to-grid interval
     * @return Pair of (new start time, new end time)
     */
    fun calculateDraggedTaskTimes(
        originalStart: LocalDateTime,
        originalEnd: LocalDateTime,
        dragDeltaY: Float,
        pixelsPerMinute: Float,
        gridMinutes: Int
    ): Pair<LocalDateTime, LocalDateTime> {
        val offsetMinutes = calculateTimeOffsetFromDrag(dragDeltaY, pixelsPerMinute, gridMinutes)

        val newStart = originalStart.plusMinutes(offsetMinutes.toLong())
        val newEnd = originalEnd.plusMinutes(offsetMinutes.toLong())

        return newStart to newEnd
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
