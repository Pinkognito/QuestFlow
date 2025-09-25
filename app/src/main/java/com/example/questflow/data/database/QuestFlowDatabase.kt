package com.example.questflow.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.questflow.data.database.dao.*
import com.example.questflow.data.database.entity.*

@Database(
    entities = [
        TaskEntity::class,
        UserStatsEntity::class,
        XpTransactionEntity::class,
        CalendarEventLinkEntity::class,
        MemeEntity::class,
        MemeUnlockEntity::class,
        SkillNodeEntity::class,
        SkillEdgeEntity::class,
        SkillUnlockEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QuestFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun xpTransactionDao(): XpTransactionDao
    abstract fun calendarEventLinkDao(): CalendarEventLinkDao
    abstract fun memeDao(): MemeDao
    abstract fun skillDao(): SkillDao

    companion object {
        const val DATABASE_NAME = "questflow_database"
    }
}