package com.example.questflow.data.database.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_28_29", "Starting migration from 28 to 29 - Action System...")

        // 1. Text Templates
        db.execSQL("""
            CREATE TABLE text_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                description TEXT,
                usageCount INTEGER NOT NULL DEFAULT 0,
                lastUsedAt TEXT,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
        """)
        Log.d("MIGRATION_28_29", "Created text_templates table")

        // 2. Text Template Tags
        db.execSQL("""
            CREATE TABLE text_template_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                templateId INTEGER NOT NULL,
                tag TEXT NOT NULL,
                FOREIGN KEY(templateId) REFERENCES text_templates(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX index_text_template_tags_templateId ON text_template_tags(templateId)")
        db.execSQL("CREATE INDEX index_text_template_tags_tag ON text_template_tags(tag)")
        Log.d("MIGRATION_28_29", "Created text_template_tags table")

        // 3. Task Contact Tags
        db.execSQL("""
            CREATE TABLE task_contact_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                contactId INTEGER NOT NULL,
                tag TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY(contactId) REFERENCES metadata_contacts(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX index_task_contact_tags_taskId ON task_contact_tags(taskId)")
        db.execSQL("CREATE INDEX index_task_contact_tags_contactId ON task_contact_tags(contactId)")
        db.execSQL("CREATE INDEX index_task_contact_tags_tag ON task_contact_tags(tag)")
        Log.d("MIGRATION_28_29", "Created task_contact_tags table")

        // 4. Tag Usage Stats
        db.execSQL("""
            CREATE TABLE tag_usage_stats (
                tag TEXT PRIMARY KEY NOT NULL,
                usageCount INTEGER NOT NULL DEFAULT 0,
                lastUsedAt TEXT,
                createdAt TEXT NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX index_tag_usage_stats_tag ON tag_usage_stats(tag)")
        db.execSQL("CREATE INDEX index_tag_usage_stats_usageCount ON tag_usage_stats(usageCount)")
        Log.d("MIGRATION_28_29", "Created tag_usage_stats table")

        // 5. Action History
        db.execSQL("""
            CREATE TABLE action_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                actionType TEXT NOT NULL,
                targetContactIds TEXT NOT NULL,
                targetContactNames TEXT NOT NULL,
                detailsJson TEXT,
                message TEXT,
                templateUsed TEXT,
                success INTEGER NOT NULL DEFAULT 1,
                errorMessage TEXT,
                executedAt TEXT NOT NULL,
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX index_action_history_taskId ON action_history(taskId)")
        db.execSQL("CREATE INDEX index_action_history_executedAt ON action_history(executedAt)")
        db.execSQL("CREATE INDEX index_action_history_actionType ON action_history(actionType)")
        Log.d("MIGRATION_28_29", "Created action_history table")

        Log.d("MIGRATION_28_29", "Migration completed successfully!")
    }
}
