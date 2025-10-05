package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataUrlEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataUrlDao {

    @Query("SELECT * FROM metadata_urls WHERE id = :id")
    suspend fun getById(id: Long): MetadataUrlEntity?

    @Query("SELECT * FROM metadata_urls WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MetadataUrlEntity?>

    @Query("SELECT * FROM metadata_urls ORDER BY title ASC")
    fun getAll(): Flow<List<MetadataUrlEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(url: MetadataUrlEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(urls: List<MetadataUrlEntity>): List<Long>

    @Update
    suspend fun update(url: MetadataUrlEntity)

    @Delete
    suspend fun delete(url: MetadataUrlEntity)

    @Query("DELETE FROM metadata_urls WHERE id = :id")
    suspend fun deleteById(id: Long)
}
