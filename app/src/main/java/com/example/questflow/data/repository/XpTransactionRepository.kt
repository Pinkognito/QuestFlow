package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.XpTransactionDao
import com.example.questflow.data.database.entity.XpTransactionEntity
import com.example.questflow.data.database.entity.XpSource
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject

class XpTransactionRepository @Inject constructor(
    private val xpTransactionDao: XpTransactionDao
) {
    suspend fun recordTransaction(
        source: XpSource,
        amount: Int,
        referenceId: Long? = null
    ) {
        xpTransactionDao.insert(
            XpTransactionEntity(
                source = source,
                amount = amount,
                referenceId = referenceId,
                timestamp = LocalDateTime.now()
            )
        )
    }

    fun getAllTransactions(): Flow<List<XpTransactionEntity>> {
        return xpTransactionDao.getAllTransactions()
    }

    suspend fun getTotalXp(): Long {
        return xpTransactionDao.getTotalXp() ?: 0L
    }
}