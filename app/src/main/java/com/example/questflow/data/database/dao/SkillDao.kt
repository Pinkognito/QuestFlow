package com.example.questflow.data.database.dao

import androidx.room.*
import com.example.questflow.data.database.entity.SkillNodeEntity
import com.example.questflow.data.database.entity.SkillEdgeEntity
import com.example.questflow.data.database.entity.SkillUnlockEntity
import com.example.questflow.data.database.entity.SkillType
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNodes(nodes: List<SkillNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEdges(edges: List<SkillEdgeEntity>)

    @Query("SELECT * FROM skill_nodes")
    fun getAllNodes(): Flow<List<SkillNodeEntity>>

    @Query("SELECT * FROM skill_edges")
    fun getAllEdges(): Flow<List<SkillEdgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun unlockNode(unlock: SkillUnlockEntity)

    @Query("SELECT * FROM skill_unlocks")
    fun getAllUnlocks(): Flow<List<SkillUnlockEntity>>

    @Query("SELECT sn.* FROM skill_nodes sn INNER JOIN skill_unlocks su ON sn.id = su.nodeId WHERE sn.type = :type")
    suspend fun getUnlockedNodesByType(type: SkillType): List<SkillNodeEntity>

    @Query("SELECT sn.* FROM skill_nodes sn INNER JOIN skill_unlocks su ON sn.id = su.nodeId")
    suspend fun getUnlockedNodes(): List<SkillNodeEntity>

    @Query("SELECT * FROM skill_edges WHERE childId = :nodeId")
    suspend fun getParentEdges(nodeId: String): List<SkillEdgeEntity>
}