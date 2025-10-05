package com.example.questflow.data.database.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d("MIGRATION_27_28", "Starting migration from 27 to 28...")

        // Create task_contact_links table
        db.execSQL("""
            CREATE TABLE task_contact_links (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                contactId INTEGER NOT NULL,
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY(contactId) REFERENCES metadata_contacts(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX index_task_contact_links_taskId ON task_contact_links(taskId)")
        db.execSQL("CREATE INDEX index_task_contact_links_contactId ON task_contact_links(contactId)")
        db.execSQL("CREATE UNIQUE INDEX index_task_contact_links_taskId_contactId ON task_contact_links(taskId, contactId)")
        Log.d("MIGRATION_27_28", "Created task_contact_links table")

        Log.d("MIGRATION_27_28", "Migration completed successfully")
    }
}
