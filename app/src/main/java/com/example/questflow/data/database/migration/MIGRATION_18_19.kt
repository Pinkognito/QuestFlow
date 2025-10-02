package com.example.questflow.data.database.migration

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_18_19", "Starting migration from version 18 to 19")

        try {
            // Add displayName and description columns to media_library table
            db.execSQL("ALTER TABLE media_library ADD COLUMN displayName TEXT NOT NULL DEFAULT ''")
            Log.d("MIGRATION_18_19", "Added displayName column to media_library")

            db.execSQL("ALTER TABLE media_library ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            Log.d("MIGRATION_18_19", "Added description column to media_library")

            Log.d("MIGRATION_18_19", "Migration from version 18 to 19 completed successfully")
        } catch (e: Exception) {
            Log.e("MIGRATION_18_19", "Error during migration from 18 to 19", e)
            throw e
        }
    }
}
