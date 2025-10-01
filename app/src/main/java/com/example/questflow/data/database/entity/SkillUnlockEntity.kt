package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "skill_unlocks")
data class SkillUnlockEntity(
    @PrimaryKey
    val nodeId: String,
    val investedPoints: Int = 1,
    val unlockedAt: LocalDateTime = LocalDateTime.now(),
    val lastInvestedAt: LocalDateTime = LocalDateTime.now()
)