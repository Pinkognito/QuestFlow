package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataAddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataAddressDao {

    @Query("SELECT * FROM metadata_addresses WHERE id = :id")
    suspend fun getById(id: Long): MetadataAddressEntity?

    @Query("SELECT * FROM metadata_addresses WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MetadataAddressEntity?>

    @Query("SELECT * FROM metadata_addresses ORDER BY city ASC, street ASC")
    fun getAll(): Flow<List<MetadataAddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: MetadataAddressEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addresses: List<MetadataAddressEntity>): List<Long>

    @Update
    suspend fun update(address: MetadataAddressEntity)

    @Delete
    suspend fun delete(address: MetadataAddressEntity)

    @Query("DELETE FROM metadata_addresses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
