package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataType
import com.example.questflow.data.database.entity.TaskMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskMetadataDao {

    @Query("SELECT * FROM task_metadata WHERE taskId = :taskId ORDER BY displayOrder ASC")
    fun getMetadataForTask(taskId: Long): Flow<List<TaskMetadataEntity>>

    @Query("SELECT * FROM task_metadata WHERE taskId = :taskId ORDER BY displayOrder ASC")
    suspend fun getMetadataForTaskSync(taskId: Long): List<TaskMetadataEntity>

    @Query("SELECT * FROM task_metadata WHERE id = :id")
    suspend fun getById(id: Long): TaskMetadataEntity?

    @Query("SELECT * FROM task_metadata WHERE taskId = :taskId AND metadataType = :type ORDER BY displayOrder ASC")
    fun getMetadataByType(taskId: Long, type: MetadataType): Flow<List<TaskMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: TaskMetadataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metadata: List<TaskMetadataEntity>): List<Long>

    @Update
    suspend fun update(metadata: TaskMetadataEntity)

    @Delete
    suspend fun delete(metadata: TaskMetadataEntity)

    @Query("DELETE FROM task_metadata WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM task_metadata WHERE taskId = :taskId")
    suspend fun deleteAllForTask(taskId: Long)

    @Query("UPDATE task_metadata SET displayOrder = :order WHERE id = :id")
    suspend fun updateDisplayOrder(id: Long, order: Int)

    @Query("SELECT COUNT(*) FROM task_metadata WHERE taskId = :taskId")
    suspend fun getMetadataCount(taskId: Long): Int
}
