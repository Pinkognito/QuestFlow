package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "category_xp_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class CategoryXpTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val xpAmount: Int,
    val source: String, // "TASK", "CALENDAR", "BONUS"
    val sourceId: Long? = null, // Reference to task or calendar event
    val multiplier: Float = 1.0f,
    val previousLevel: Int,
    val newLevel: Int,
    val previousTotalXp: Int,
    val newTotalXp: Int,
    val timestamp: LocalDateTime = LocalDateTime.now()
)