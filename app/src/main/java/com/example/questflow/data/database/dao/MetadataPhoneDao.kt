package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataPhoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataPhoneDao {

    @Query("SELECT * FROM metadata_phones WHERE id = :id")
    suspend fun getById(id: Long): MetadataPhoneEntity?

    @Query("SELECT * FROM metadata_phones WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MetadataPhoneEntity?>

    @Query("SELECT * FROM metadata_phones ORDER BY phoneNumber ASC")
    fun getAll(): Flow<List<MetadataPhoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phone: MetadataPhoneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(phones: List<MetadataPhoneEntity>): List<Long>

    @Update
    suspend fun update(phone: MetadataPhoneEntity)

    @Delete
    suspend fun delete(phone: MetadataPhoneEntity)

    @Query("DELETE FROM metadata_phones WHERE id = :id")
    suspend fun deleteById(id: Long)
}
