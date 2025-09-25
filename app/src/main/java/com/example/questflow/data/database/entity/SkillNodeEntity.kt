package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skill_nodes")
data class SkillNodeEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val type: SkillType,
    val value: Float
)

enum class SkillType {
    XP_MULT, STREAK_GUARD, EXTRA_MEME
}