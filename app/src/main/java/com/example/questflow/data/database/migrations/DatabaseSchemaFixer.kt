package com.example.questflow.data.database.migrations

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Automatic database schema fixer that ensures all columns exist.
 *
 * This class automatically adds missing columns to the database tables
 * to prevent issues when migrations weren't run (e.g., due to reinstallation
 * or database imports from older versions).
 *
 * Usage: Call fixSchema() after database creation but before any queries.
 */
object DatabaseSchemaFixer {
    private const val TAG = "DatabaseSchemaFixer"

    /**
     * Ensures all required columns exist in all tables.
     * Safe to call multiple times - will only add missing columns.
     */
    fun fixSchema(db: SupportSQLiteDatabase) {
        Log.d(TAG, "🔧 Checking database schema for missing columns...")

        // Fix tasks table
        ensureTasksColumns(db)

        // Add more table fixes here as needed
        // ensureCalendarLinksColumns(db)
        // ensureContactsColumns(db)

        Log.d(TAG, "✅ Database schema check complete")
    }

    private fun ensureTasksColumns(db: SupportSQLiteDatabase) {
        val tableName = "tasks"
        Log.d(TAG, "Checking table: $tableName")

        val existingColumns = getTableColumns(db, tableName)

        // Define all columns that should exist (column name -> SQL definition)
        val requiredColumns = mapOf(
            "triggerMode" to "TEXT DEFAULT NULL",
            "specificTime" to "TEXT DEFAULT NULL",
            "recurringType" to "TEXT DEFAULT NULL",
            "recurringInterval" to "INTEGER DEFAULT NULL",
            "recurringDays" to "TEXT DEFAULT NULL",
            "lastCompletedAt" to "INTEGER DEFAULT NULL",
            "nextDueDate" to "INTEGER DEFAULT NULL"
            // Add more columns as needed
        )

        // Add missing columns
        requiredColumns.forEach { (columnName, columnDef) ->
            if (!existingColumns.contains(columnName)) {
                Log.w(TAG, "⚠️ Missing column: $tableName.$columnName - adding it now")
                try {
                    db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDef")
                    Log.d(TAG, "✅ Added column: $tableName.$columnName")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to add column $tableName.$columnName", e)
                }
            } else {
                Log.d(TAG, "  ✓ Column exists: $tableName.$columnName")
            }
        }
    }

    /**
     * Gets all column names for a given table.
     */
    private fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val columns = mutableSetOf<String>()
        var cursor: Cursor? = null

        try {
            cursor = db.query("PRAGMA table_info($tableName)")
            val nameIndex = cursor.getColumnIndex("name")

            if (nameIndex >= 0) {
                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(nameIndex)
                    columns.add(columnName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get columns for table $tableName", e)
        } finally {
            cursor?.close()
        }

        return columns
    }

    /**
     * Checks if a specific column exists in a table.
     */
    private fun columnExists(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
        return getTableColumns(db, tableName).contains(columnName)
    }
}
