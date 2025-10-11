package com.example.questflow.di

import android.content.Context
import androidx.room.Room
import com.example.questflow.data.database.QuestFlowDatabase
import com.example.questflow.data.database.TaskDao
import com.example.questflow.data.database.dao.*
import com.example.questflow.data.database.migration.MIGRATION_1_2
import com.example.questflow.data.database.migration.MIGRATION_2_3
import com.example.questflow.data.database.migration.MIGRATION_3_4
import com.example.questflow.data.database.migration.MIGRATION_4_5
import com.example.questflow.data.database.migration.MIGRATION_5_6
import com.example.questflow.data.database.migration.MIGRATION_6_7
import com.example.questflow.data.database.migration.MIGRATION_7_8
import com.example.questflow.data.database.migration.MIGRATION_8_9
import com.example.questflow.data.database.migration.MIGRATION_9_10
import com.example.questflow.data.database.migration.MIGRATION_10_11
import com.example.questflow.data.database.migration.MIGRATION_11_12
import com.example.questflow.data.database.migration.MIGRATION_12_13
import com.example.questflow.data.database.migration.MIGRATION_13_14
import com.example.questflow.data.database.migration.MIGRATION_14_15
import com.example.questflow.data.database.migration.MIGRATION_15_16
import com.example.questflow.data.database.migration.MIGRATION_16_17
import com.example.questflow.data.database.migration.MIGRATION_17_18
import com.example.questflow.data.database.migration.MIGRATION_18_19
import com.example.questflow.data.database.migration.MIGRATION_19_20
import com.example.questflow.data.database.migration.MIGRATION_20_21
import com.example.questflow.data.database.migration.MIGRATION_21_22
import com.example.questflow.data.database.migration.MIGRATION_22_23
import com.example.questflow.data.database.migration.MIGRATION_23_24
import com.example.questflow.data.database.migration.MIGRATION_24_25
import com.example.questflow.data.database.migration.MIGRATION_25_26
import com.example.questflow.data.database.migrations.MIGRATION_26_27
import com.example.questflow.data.database.migrations.MIGRATION_27_28
import com.example.questflow.data.database.migrations.MIGRATION_28_29
import com.example.questflow.data.database.migrations.MIGRATION_29_30
import com.example.questflow.data.database.migrations.MIGRATION_30_31
import com.example.questflow.data.database.migrations.MIGRATION_31_32
import com.example.questflow.data.database.migrations.MIGRATION_32_33
import com.example.questflow.data.database.migrations.MIGRATION_33_34
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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34)
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
    fun provideMemeDao(database: QuestFlowDatabase): MemeDao {
        return database.memeDao()
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

    @Provides
    fun provideDynamicChartDao(database: QuestFlowDatabase): DynamicChartDao {
        return database.dynamicChartDao()
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
}