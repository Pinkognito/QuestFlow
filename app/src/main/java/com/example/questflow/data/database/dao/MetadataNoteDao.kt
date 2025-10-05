package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataNoteDao {

    @Query("SELECT * FROM metadata_notes WHERE id = :id")
    suspend fun getById(id: Long): MetadataNoteEntity?

    @Query("SELECT * FROM metadata_notes WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MetadataNoteEntity?>

    @Query("SELECT * FROM metadata_notes ORDER BY isPinned DESC, id DESC")
    fun getAll(): Flow<List<MetadataNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: MetadataNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<MetadataNoteEntity>): List<Long>

    @Update
    suspend fun update(note: MetadataNoteEntity)

    @Delete
    suspend fun delete(note: MetadataNoteEntity)

    @Query("DELETE FROM metadata_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
