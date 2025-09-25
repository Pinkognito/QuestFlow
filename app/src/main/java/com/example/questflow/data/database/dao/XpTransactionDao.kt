package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.XpTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XpTransactionDao {
    @Insert
    suspend fun insert(transaction: XpTransactionEntity)

    @Query("SELECT * FROM xp_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<XpTransactionEntity>>

    @Query("SELECT * FROM xp_transactions WHERE referenceId = :refId")
    suspend fun getByReferenceId(refId: Long): List<XpTransactionEntity>

    @Query("SELECT SUM(amount) FROM xp_transactions")
    suspend fun getTotalXp(): Long?
}