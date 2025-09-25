package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.MemeDao
import com.example.questflow.data.database.entity.MemeEntity
import com.example.questflow.data.database.entity.MemeUnlockEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class MemeWithUnlock(
    val meme: MemeEntity,
    val isUnlocked: Boolean,
    val unlockedAt: java.time.LocalDateTime? = null
)

class MemeRepository @Inject constructor(
    private val memeDao: MemeDao
) {
    fun getAllMemesWithUnlocks(): Flow<List<MemeWithUnlock>> {
        return combine(
            memeDao.getAllMemesFlow(),
            memeDao.getAllUnlocks()
        ) { memes, unlocks ->
            val unlockMap = unlocks.associateBy { it.memeId }
            memes.map { meme ->
                val unlock = unlockMap[meme.id]
                MemeWithUnlock(
                    meme = meme,
                    isUnlocked = unlock != null,
                    unlockedAt = unlock?.unlockedAt
                )
            }
        }
    }

    suspend fun unlockNextMeme(levelAtUnlock: Int): MemeEntity? {
        val nextMeme = memeDao.getNextLockedMeme()
        if (nextMeme != null) {
            memeDao.insertUnlock(
                MemeUnlockEntity(
                    memeId = nextMeme.id,
                    levelAtUnlock = levelAtUnlock
                )
            )
        }
        return nextMeme
    }

    suspend fun getUnlockedCount(): Int {
        return memeDao.getUnlockedCount()
    }
}