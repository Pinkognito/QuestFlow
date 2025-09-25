package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memes")
data class MemeEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val description: String,
    val imageResourceId: Int,
    val rarity: String,
    val requiredLevel: Int,
    val title: String = "",  // For backward compatibility
    val resourceName: String = ""  // For backward compatibility
)