package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Int = 0,
    val xp: Long = 0,
    val level: Int = 1,
    val points: Int = 0
)