package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "collection_unlocks",
    foreignKeys = [
        ForeignKey(
            entity = CollectionItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CollectionUnlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val collectionItemId: Long,
    val levelAtUnlock: Int,
    val unlockedAt: LocalDateTime = LocalDateTime.now()
)
