package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val color: String = "#2196F3", // Default blue color
    val emoji: String = "🎯", // Default emoji
    val currentXp: Int = 0,
    val currentLevel: Int = 1,
    val totalXp: Int = 0,
    val skillPoints: Int = 0,
    val levelScalingFactor: Float = 1.0f, // Factor to scale XP requirements (0.5 = easier, 2.0 = harder)
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)