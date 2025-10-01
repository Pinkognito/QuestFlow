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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
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
}