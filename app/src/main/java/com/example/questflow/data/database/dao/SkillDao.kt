package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.SkillNodeEntity
import com.example.questflow.data.database.entity.SkillEdgeEntity
import com.example.questflow.data.database.entity.SkillUnlockEntity
import com.example.questflow.data.database.entity.SkillType
import com.example.questflow.data.database.entity.SkillEffectType
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    // === Node Management ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: SkillNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<SkillNodeEntity>)

    @Update
    suspend fun updateNode(node: SkillNodeEntity)

    @Query("DELETE FROM skill_nodes WHERE id = :nodeId")
    suspend fun deleteNode(nodeId: String)

    @Query("SELECT * FROM skill_nodes WHERE id = :nodeId")
    suspend fun getNodeById(nodeId: String): SkillNodeEntity?

    @Query("SELECT * FROM skill_nodes")
    fun getAllNodes(): Flow<List<SkillNodeEntity>>

    @Query("SELECT * FROM skill_nodes WHERE categoryId = :categoryId")
    fun getNodesByCategory(categoryId: Long): Flow<List<SkillNodeEntity>>

    @Query("SELECT * FROM skill_nodes WHERE categoryId IS NULL")
    fun getGlobalNodes(): Flow<List<SkillNodeEntity>>

    // === Edge Management ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: SkillEdgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<SkillEdgeEntity>)

    @Query("DELETE FROM skill_edges WHERE parentId = :parentId AND childId = :childId")
    suspend fun deleteEdge(parentId: String, childId: String)

    @Query("DELETE FROM skill_edges WHERE childId = :nodeId OR parentId = :nodeId")
    suspend fun deleteAllEdgesForNode(nodeId: String)

    @Query("SELECT * FROM skill_edges")
    fun getAllEdges(): Flow<List<SkillEdgeEntity>>

    @Query("SELECT * FROM skill_edges WHERE childId = :nodeId")
    suspend fun getParentEdges(nodeId: String): List<SkillEdgeEntity>

    @Query("SELECT * FROM skill_edges WHERE parentId = :nodeId")
    suspend fun getChildEdges(nodeId: String): List<SkillEdgeEntity>

    // === Unlock/Investment Management ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUnlock(unlock: SkillUnlockEntity)

    @Query("SELECT * FROM skill_unlocks WHERE nodeId = :nodeId")
    suspend fun getUnlock(nodeId: String): SkillUnlockEntity?

    @Query("SELECT * FROM skill_unlocks")
    fun getAllUnlocks(): Flow<List<SkillUnlockEntity>>

    @Query("DELETE FROM skill_unlocks WHERE nodeId = :nodeId")
    suspend fun deleteUnlock(nodeId: String)

    // === Query Unlocked Nodes ===
    @Query("SELECT sn.* FROM skill_nodes sn INNER JOIN skill_unlocks su ON sn.id = su.nodeId")
    suspend fun getUnlockedNodes(): List<SkillNodeEntity>

    @Query("SELECT sn.* FROM skill_nodes sn INNER JOIN skill_unlocks su ON sn.id = su.nodeId WHERE sn.effectType = :effectType")
    suspend fun getUnlockedNodesByEffectType(effectType: SkillEffectType): List<SkillNodeEntity>

    // === Investment Queries ===
    @Query("""
        SELECT su.investedPoints
        FROM skill_unlocks su
        WHERE su.nodeId = :nodeId
    """)
    suspend fun getInvestedPoints(nodeId: String): Int?
}