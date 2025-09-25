package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "skill_unlocks")
data class SkillUnlockEntity(
    @PrimaryKey
    val nodeId: String,
    val unlockedAt: LocalDateTime = LocalDateTime.now()
)