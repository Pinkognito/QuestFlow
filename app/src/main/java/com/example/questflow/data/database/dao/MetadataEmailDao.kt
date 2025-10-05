package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataEmailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataEmailDao {

    @Query("SELECT * FROM metadata_emails WHERE id = :id")
    suspend fun getById(id: Long): MetadataEmailEntity?

    @Query("SELECT * FROM metadata_emails WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MetadataEmailEntity?>

    @Query("SELECT * FROM metadata_emails ORDER BY emailAddress ASC")
    fun getAll(): Flow<List<MetadataEmailEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(email: MetadataEmailEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emails: List<MetadataEmailEntity>): List<Long>

    @Update
    suspend fun update(email: MetadataEmailEntity)

    @Delete
    suspend fun delete(email: MetadataEmailEntity)

    @Query("DELETE FROM metadata_emails WHERE id = :id")
    suspend fun deleteById(id: Long)
}
