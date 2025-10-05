package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.WidgetConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_configs WHERE isVisible = 1 ORDER BY position ASC")
    fun getAllWidgets(): Flow<List<WidgetConfigEntity>>

    @Query("SELECT * FROM widget_configs ORDER BY position ASC")
    suspend fun getAllWidgetsOnce(): List<WidgetConfigEntity>

    @Query("SELECT * FROM widget_configs WHERE id = :widgetId")
    suspend fun getWidgetById(widgetId: Long): WidgetConfigEntity?

    @Insert
    suspend fun insertWidget(widget: WidgetConfigEntity): Long

    @Update
    suspend fun updateWidget(widget: WidgetConfigEntity)

    @Delete
    suspend fun deleteWidget(widget: WidgetConfigEntity)

    @Query("DELETE FROM widget_configs WHERE id = :widgetId")
    suspend fun deleteWidgetById(widgetId: Long)

    @Query("UPDATE widget_configs SET position = :newPosition WHERE id = :widgetId")
    suspend fun updateWidgetPosition(widgetId: Long, newPosition: Int)

    @Transaction
    suspend fun reorderWidgets(widgetIds: List<Long>) {
        widgetIds.forEachIndexed { index, widgetId ->
            updateWidgetPosition(widgetId, index)
        }
    }
}
