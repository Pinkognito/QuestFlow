package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MetadataLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataLocationDao {

    @Query("SELECT * FROM metadata_locations WHERE id = :id")
    suspend fun getById(id: Long): MetadataLocationEntity?

    @Query("SELECT * FROM metadata_locations WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MetadataLocationEntity?>

    @Query("SELECT * FROM metadata_locations WHERE placeId = :placeId")
    suspend fun getByPlaceId(placeId: String): MetadataLocationEntity?

    @Query("SELECT * FROM metadata_locations ORDER BY placeName ASC")
    fun getAll(): Flow<List<MetadataLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: MetadataLocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<MetadataLocationEntity>): List<Long>

    @Update
    suspend fun update(location: MetadataLocationEntity)

    @Delete
    suspend fun delete(location: MetadataLocationEntity)

    @Query("DELETE FROM metadata_locations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
