package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.MemeEntity
import com.example.questflow.data.database.entity.MemeUnlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeme(meme: MemeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemes(memes: List<MemeEntity>)

    @Query("SELECT * FROM memes ORDER BY id")
    fun getAllMemesFlow(): Flow<List<MemeEntity>>

    @Query("SELECT * FROM memes ORDER BY id")
    suspend fun getAllMemes(): List<MemeEntity>

    @Query("SELECT m.* FROM memes m LEFT JOIN meme_unlocks mu ON m.id = mu.memeId WHERE mu.id IS NULL ORDER BY m.id LIMIT 1")
    suspend fun getNextLockedMeme(): MemeEntity?

    @Insert
    suspend fun insertUnlock(unlock: MemeUnlockEntity)

    @Query("SELECT * FROM meme_unlocks ORDER BY unlockedAt DESC")
    fun getAllUnlocks(): Flow<List<MemeUnlockEntity>>

    @Query("SELECT COUNT(*) FROM meme_unlocks")
    suspend fun getUnlockedCount(): Int
}