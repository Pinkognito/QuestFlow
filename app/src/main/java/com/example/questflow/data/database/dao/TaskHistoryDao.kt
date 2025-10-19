package com.example.questflow.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.questflow.data.database.entity.TaskHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TaskHistoryDao {

    @Insert
    suspend fun insert(entry: TaskHistoryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<TaskHistoryEntity>)

    /**
     * Prüfe ob ein identisches Event bereits existiert (innerhalb von 5 Sekunden)
     */
    @Query("""
        SELECT COUNT(*) FROM task_history
        WHERE taskId = :taskId
        AND eventType = :eventType
        AND ABS((julianday(:timestamp) - julianday(timestamp)) * 86400) < 5
    """)
    suspend fun countSimilarEvents(taskId: Long, eventType: String, timestamp: LocalDateTime): Int

    /**
     * Füge Event nur ein, wenn es nicht bereits existiert (verhindert Duplikate)
     */
    suspend fun insertIfNotExists(entry: TaskHistoryEntity): Long {
        val existingCount = countSimilarEvents(entry.taskId, entry.eventType, entry.timestamp)
        return if (existingCount == 0) {
            insert(entry)
        } else {
            -1L // Bereits vorhanden
        }
    }

    /**
     * Hole alle History-Einträge für einen bestimmten Task
     */
    @Query("SELECT * FROM task_history WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getHistoryForTask(taskId: Long): Flow<List<TaskHistoryEntity>>

    /**
     * Hole History-Einträge eines bestimmten Typs
     */
    @Query("SELECT * FROM task_history WHERE eventType = :eventType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getHistoryByType(eventType: String, limit: Int = 100): List<TaskHistoryEntity>

    /**
     * Zähle Events pro Typ (für Statistiken)
     */
    @Query("SELECT COUNT(*) FROM task_history WHERE eventType = :eventType")
    suspend fun countByType(eventType: String): Int

    /**
     * Zähle Events pro Typ in einem Zeitraum
     */
    @Query("""
        SELECT COUNT(*) FROM task_history
        WHERE eventType = :eventType
        AND timestamp >= :since
    """)
    suspend fun countByTypeSince(eventType: String, since: LocalDateTime): Int

    /**
     * Lösche History älter als gegebenes Datum (Auto-Cleanup)
     */
    @Query("DELETE FROM task_history WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: LocalDateTime): Int

    /**
     * Behalte nur die letzten N Einträge pro Task
     */
    @Query("""
        DELETE FROM task_history
        WHERE id NOT IN (
            SELECT id FROM task_history
            WHERE taskId = :taskId
            ORDER BY timestamp DESC
            LIMIT :keepCount
        ) AND taskId = :taskId
    """)
    suspend fun keepLatestNForTask(taskId: Long, keepCount: Int = 10)

    /**
     * Hole Statistik: Anzahl Events pro Tag für einen Zeitraum
     */
    @Query("""
        SELECT DATE(timestamp) as date, eventType, COUNT(*) as count
        FROM task_history
        WHERE timestamp >= :since
        GROUP BY DATE(timestamp), eventType
        ORDER BY date DESC
    """)
    suspend fun getDailyStatistics(since: LocalDateTime): List<DailyHistoryStatistic>

    /**
     * Lösche alle History-Einträge (für Testing)
     */
    @Query("DELETE FROM task_history")
    suspend fun deleteAll()
}

/**
 * Data class für Statistiken
 */
data class DailyHistoryStatistic(
    val date: String,
    val eventType: String,
    val count: Int
)
