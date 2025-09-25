package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "meme_unlocks")
data class MemeUnlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memeId: Int,
    val unlockedAt: LocalDateTime = LocalDateTime.now(),
    val levelAtUnlock: Int
)