package com.example.questflow.di

import android.content.Context
import androidx.room.Room
import com.example.questflow.data.database.QuestFlowDatabase
import com.example.questflow.data.database.TaskDao
import com.example.questflow.data.database.dao.*
import com.example.questflow.data.database.migrations.MIGRATION_1_2
import com.example.questflow.data.database.migrations.MIGRATION_2_3
import com.example.questflow.data.database.migrations.MIGRATION_3_4
import com.example.questflow.data.database.migrations.MIGRATION_4_5
import com.example.questflow.data.database.migrations.MIGRATION_5_6
import com.example.questflow.data.database.migrations.MIGRATION_6_7
import com.example.questflow.data.database.migrations.MIGRATION_7_8
import com.example.questflow.data.database.migrations.MIGRATION_8_9
import com.example.questflow.data.database.migrations.MIGRATION_9_10
import com.example.questflow.data.database.migrations.MIGRATION_10_11
import com.example.questflow.data.database.migrations.MIGRATION_11_12
import com.example.questflow.data.database.migrations.MIGRATION_12_13
import com.example.questflow.data.database.migrations.MIGRATION_13_14
import com.example.questflow.data.database.migrations.MIGRATION_14_15
import com.example.questflow.data.database.migrations.MIGRATION_15_16
import com.example.questflow.data.database.migrations.MIGRATION_16_17
import com.example.questflow.data.database.migrations.MIGRATION_17_18
import com.example.questflow.data.database.migrations.MIGRATION_18_19
import com.example.questflow.data.database.migrations.MIGRATION_19_20
import com.example.questflow.data.database.migrations.MIGRATION_20_21
import com.example.questflow.data.database.migrations.MIGRATION_21_22
import com.example.questflow.data.database.migrations.MIGRATION_22_23
import com.example.questflow.data.database.migrations.MIGRATION_23_24
import com.example.questflow.data.database.migrations.MIGRATION_24_25
import com.example.questflow.data.database.migrations.MIGRATION_25_26
import com.example.questflow.data.database.migrations.MIGRATION_26_27
import com.example.questflow.data.database.migrations.MIGRATION_27_28
import com.example.questflow.data.database.migrations.MIGRATION_28_29
import com.example.questflow.data.database.migrations.MIGRATION_29_30
import com.example.questflow.data.database.migrations.MIGRATION_30_31
import com.example.questflow.data.database.migrations.MIGRATION_31_32
import com.example.questflow.data.database.migrations.MIGRATION_32_33
import com.example.questflow.data.database.migrations.MIGRATION_33_34
import com.example.questflow.data.database.migrations.MIGRATION_34_35
import com.example.questflow.data.database.migrations.MIGRATION_35_36
import com.example.questflow.data.database.migrations.MIGRATION_36_37
import com.example.questflow.data.database.migrations.MIGRATION_37_38
import com.example.questflow.data.database.migrations.MIGRATION_38_39
import com.example.questflow.data.database.migrations.MIGRATION_39_40
import com.example.questflow.data.database.migrations.MIGRATION_40_41
import com.example.questflow.data.database.migrations.MIGRATION_42_43
import com.example.questflow.data.database.migrations.MIGRATION_43_44
import com.example.questflow.data.database.migrations.MIGRATION_44_45
import com.example.questflow.data.database.migrations.MIGRATION_45_46
import com.example.questflow.data.database.migrations.MIGRATION_46_47
import com.example.questflow.data.database.migrations.DatabaseSchemaFixer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideQuestFlowDatabase(
        @ApplicationContext context: Context
    ): QuestFlowDatabase {
        return Room.databaseBuilder(
            context,
            QuestFlowDatabase::class.java,
            QuestFlowDatabase.DATABASE_NAME
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37, MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40, MIGRATION_40_41, com.example.questflow.data.database.migrations.MIGRATION_41_42, MIGRATION_42_43, MIGRATION_43_44, MIGRATION_44_45, MIGRATION_45_46, MIGRATION_46_47)
        .addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onOpen(db)
                // Automatically fix schema on every app start
                DatabaseSchemaFixer.fixSchema(db)
            }
        })
        .build()
    }

    @Provides
    fun provideTaskDao(database: QuestFlowDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideUserStatsDao(database: QuestFlowDatabase): UserStatsDao {
        return database.userStatsDao()
    }

    @Provides
    fun provideXpTransactionDao(database: QuestFlowDatabase): XpTransactionDao {
        return database.xpTransactionDao()
    }

    @Provides
    fun provideCalendarEventLinkDao(database: QuestFlowDatabase): CalendarEventLinkDao {
        return database.calendarEventLinkDao()
    }

    @Provides
    fun provideCollectionDao(database: QuestFlowDatabase): CollectionDao {
        return database.collectionDao()
    }

    @Provides
    fun provideSkillDao(database: QuestFlowDatabase): SkillDao {
        return database.skillDao()
    }

    @Provides
    fun provideCategoryDao(database: QuestFlowDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideMediaLibraryDao(database: QuestFlowDatabase): MediaLibraryDao {
        return database.mediaLibraryDao()
    }

    @Provides
    fun provideMediaUsageDao(database: QuestFlowDatabase): MediaUsageDao {
        return database.mediaUsageDao()
    }

    @Provides
    fun provideStatisticsDao(database: QuestFlowDatabase): StatisticsDao {
        return database.statisticsDao()
    }

    // Task Metadata DAO Providers
    @Provides
    fun provideTaskMetadataDao(database: QuestFlowDatabase): TaskMetadataDao {
        return database.taskMetadataDao()
    }

    @Provides
    fun provideMetadataLocationDao(database: QuestFlowDatabase): MetadataLocationDao {
        return database.metadataLocationDao()
    }

    @Provides
    fun provideMetadataContactDao(database: QuestFlowDatabase): MetadataContactDao {
        return database.metadataContactDao()
    }

    @Provides
    fun provideMetadataPhoneDao(database: QuestFlowDatabase): MetadataPhoneDao {
        return database.metadataPhoneDao()
    }

    @Provides
    fun provideMetadataAddressDao(database: QuestFlowDatabase): MetadataAddressDao {
        return database.metadataAddressDao()
    }

    @Provides
    fun provideMetadataEmailDao(database: QuestFlowDatabase): MetadataEmailDao {
        return database.metadataEmailDao()
    }

    @Provides
    fun provideMetadataUrlDao(database: QuestFlowDatabase): MetadataUrlDao {
        return database.metadataUrlDao()
    }

    @Provides
    fun provideMetadataNoteDao(database: QuestFlowDatabase): MetadataNoteDao {
        return database.metadataNoteDao()
    }

    @Provides
    fun provideMetadataFileAttachmentDao(database: QuestFlowDatabase): MetadataFileAttachmentDao {
        return database.metadataFileAttachmentDao()
    }

    @Provides
    fun provideTaskContactLinkDao(database: QuestFlowDatabase): TaskContactLinkDao {
        return database.taskContactLinkDao()
    }

    // Action System DAO Providers
    @Provides
    fun provideTextTemplateDao(database: QuestFlowDatabase): TextTemplateDao {
        return database.textTemplateDao()
    }

    @Provides
    fun provideTextTemplateTagDao(database: QuestFlowDatabase): TextTemplateTagDao {
        return database.textTemplateTagDao()
    }

    @Provides
    fun provideTaskContactTagDao(database: QuestFlowDatabase): TaskContactTagDao {
        return database.taskContactTagDao()
    }

    @Provides
    fun provideTagUsageStatsDao(database: QuestFlowDatabase): TagUsageStatsDao {
        return database.tagUsageStatsDao()
    }

    @Provides
    fun provideActionHistoryDao(database: QuestFlowDatabase): ActionHistoryDao {
        return database.actionHistoryDao()
    }

    // Global Tag System DAO Providers
    @Provides
    fun provideMetadataTagDao(database: QuestFlowDatabase): MetadataTagDao {
        return database.metadataTagDao()
    }

    @Provides
    fun provideContactTagDao(database: QuestFlowDatabase): ContactTagDao {
        return database.contactTagDao()
    }

    // Task Search Filter Settings DAO Provider
    @Provides
    fun provideTaskSearchFilterSettingsDao(database: QuestFlowDatabase): TaskSearchFilterSettingsDao {
        return database.taskSearchFilterSettingsDao()
    }

    // Task Display Settings DAO Provider
    @Provides
    fun provideTaskDisplaySettingsDao(database: QuestFlowDatabase): TaskDisplaySettingsDao {
        return database.taskDisplaySettingsDao()
    }

    // Task Filter Preset DAO Provider
    @Provides
    fun provideTaskFilterPresetDao(database: QuestFlowDatabase): TaskFilterPresetDao {
        return database.taskFilterPresetDao()
    }

    // Working Hours Settings DAO Provider
    @Provides
    fun provideWorkingHoursSettingsDao(database: QuestFlowDatabase): WorkingHoursSettingsDao {
        return database.workingHoursSettingsDao()
    }

    // Task History DAO Provider
    @Provides
    fun provideTaskHistoryDao(database: QuestFlowDatabase): TaskHistoryDao {
        return database.taskHistoryDao()
    }
}