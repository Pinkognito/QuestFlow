package com.example.questflow.data.database.migration

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_20_21", "Starting migration from version 20 to 21")

        try {
            // Fix completed tasks without completedAt timestamp
            // Use createdAt as a fallback estimate for when the task was completed
            db.execSQL("""
                UPDATE tasks
                SET completedAt = createdAt
                WHERE isCompleted = 1 AND completedAt IS NULL
            """)
            Log.d("MIGRATION_20_21", "Fixed completed tasks without completedAt timestamp")

            Log.d("MIGRATION_20_21", "Migration from version 20 to 21 completed successfully")
        } catch (e: Exception) {
            Log.e("MIGRATION_20_21", "Error during migration from 20 to 21", e)
            throw e
        }
    }
}
