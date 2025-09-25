package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "xp_transactions")
data class XpTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val source: XpSource,
    val referenceId: Long? = null,
    val amount: Int
)

enum class XpSource {
    TASK, CALENDAR, SYSTEM, SUBTASK
}