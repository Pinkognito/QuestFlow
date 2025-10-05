package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataFileAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataFileAttachmentDao {

    @Query("SELECT * FROM metadata_file_attachments WHERE id = :id")
    suspend fun getById(id: Long): MetadataFileAttachmentEntity?

    @Query("SELECT * FROM metadata_file_attachments WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MetadataFileAttachmentEntity?>

    @Query("SELECT * FROM metadata_file_attachments ORDER BY fileName ASC")
    fun getAll(): Flow<List<MetadataFileAttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: MetadataFileAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<MetadataFileAttachmentEntity>): List<Long>

    @Update
    suspend fun update(file: MetadataFileAttachmentEntity)

    @Delete
    suspend fun delete(file: MetadataFileAttachmentEntity)

    @Query("DELETE FROM metadata_file_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
