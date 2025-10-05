package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataContactDao {

    @Query("SELECT * FROM metadata_contacts WHERE id = :id")
    suspend fun getById(id: Long): MetadataContactEntity?

    @Query("SELECT * FROM metadata_contacts WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MetadataContactEntity?>

    @Query("SELECT * FROM metadata_contacts WHERE systemContactId = :systemContactId")
    suspend fun getBySystemContactId(systemContactId: String): MetadataContactEntity?

    @Query("SELECT * FROM metadata_contacts ORDER BY displayName ASC")
    fun getAll(): Flow<List<MetadataContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: MetadataContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<MetadataContactEntity>): List<Long>

    @Update
    suspend fun update(contact: MetadataContactEntity)

    @Delete
    suspend fun delete(contact: MetadataContactEntity)

    @Query("DELETE FROM metadata_contacts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
