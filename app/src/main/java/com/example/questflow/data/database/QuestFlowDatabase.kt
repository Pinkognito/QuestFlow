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
        CollectionItemEntity::class,
        CollectionUnlockEntity::class,
        SkillNodeEntity::class,
        SkillEdgeEntity::class,
        SkillUnlockEntity::class,
        CategoryEntity::class,
        CategoryXpTransactionEntity::class,
        MediaLibraryEntity::class,
        MediaUsageEntity::class,
        StatisticsConfigEntity::class,
        DynamicChartEntity::class,
        // Task Metadata System
        TaskMetadataEntity::class,
        MetadataLocationEntity::class,
        MetadataContactEntity::class,
        MetadataPhoneEntity::class,
        MetadataAddressEntity::class,
        MetadataEmailEntity::class,
        MetadataUrlEntity::class,
        MetadataNoteEntity::class,
        MetadataFileAttachmentEntity::class
    ],
    version = 26,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QuestFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun xpTransactionDao(): XpTransactionDao
    abstract fun calendarEventLinkDao(): CalendarEventLinkDao
    abstract fun memeDao(): MemeDao
    abstract fun collectionDao(): CollectionDao
    abstract fun skillDao(): SkillDao
    abstract fun categoryDao(): CategoryDao
    abstract fun mediaLibraryDao(): MediaLibraryDao
    abstract fun mediaUsageDao(): MediaUsageDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun dynamicChartDao(): DynamicChartDao

    // Task Metadata DAOs
    abstract fun taskMetadataDao(): TaskMetadataDao
    abstract fun metadataLocationDao(): MetadataLocationDao
    abstract fun metadataContactDao(): MetadataContactDao
    abstract fun metadataPhoneDao(): MetadataPhoneDao
    abstract fun metadataAddressDao(): MetadataAddressDao
    abstract fun metadataEmailDao(): MetadataEmailDao
    abstract fun metadataUrlDao(): MetadataUrlDao
    abstract fun metadataNoteDao(): MetadataNoteDao
    abstract fun metadataFileAttachmentDao(): MetadataFileAttachmentDao

    companion object {
        const val DATABASE_NAME = "questflow_database"
    }
}