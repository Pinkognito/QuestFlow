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
        // Task Metadata System
        TaskMetadataEntity::class,
        MetadataLocationEntity::class,
        MetadataContactEntity::class,
        MetadataPhoneEntity::class,
        MetadataAddressEntity::class,
        MetadataEmailEntity::class,
        MetadataUrlEntity::class,
        MetadataNoteEntity::class,
        MetadataFileAttachmentEntity::class,
        TaskContactLinkEntity::class,
        // Action System
        TextTemplateEntity::class,
        TextTemplateTagEntity::class,
        TaskContactTagEntity::class,
        TagUsageStatsEntity::class,
        ActionHistoryEntity::class,
        // Global Tag System
        MetadataTagEntity::class,
        ContactTagEntity::class,
        // Task Search Filter Settings
        TaskSearchFilterSettingsEntity::class,
        // Task Display Settings
        TaskDisplaySettingsEntity::class,
        // Task Filter Presets
        TaskFilterPresetEntity::class
    ],
    version = 41,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QuestFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun xpTransactionDao(): XpTransactionDao
    abstract fun calendarEventLinkDao(): CalendarEventLinkDao
    abstract fun collectionDao(): CollectionDao
    abstract fun skillDao(): SkillDao
    abstract fun categoryDao(): CategoryDao
    abstract fun mediaLibraryDao(): MediaLibraryDao
    abstract fun mediaUsageDao(): MediaUsageDao
    abstract fun statisticsDao(): StatisticsDao

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
    abstract fun taskContactLinkDao(): TaskContactLinkDao

    // Action System DAOs
    abstract fun textTemplateDao(): TextTemplateDao
    abstract fun textTemplateTagDao(): TextTemplateTagDao
    abstract fun taskContactTagDao(): TaskContactTagDao
    abstract fun tagUsageStatsDao(): TagUsageStatsDao
    abstract fun actionHistoryDao(): ActionHistoryDao

    // Global Tag System DAOs
    abstract fun metadataTagDao(): MetadataTagDao
    abstract fun contactTagDao(): ContactTagDao

    // Task Search Filter Settings DAO
    abstract fun taskSearchFilterSettingsDao(): TaskSearchFilterSettingsDao

    // Task Display Settings DAO
    abstract fun taskDisplaySettingsDao(): TaskDisplaySettingsDao

    // Task Filter Presets DAO
    abstract fun taskFilterPresetDao(): TaskFilterPresetDao

    companion object {
        const val DATABASE_NAME = "questflow_database"
    }
}