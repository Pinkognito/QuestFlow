package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.SkillDao
import com.example.questflow.data.database.entity.SkillNodeEntity
import com.example.questflow.data.database.entity.SkillType
import com.example.questflow.data.database.entity.SkillUnlockEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class SkillNodeWithStatus(
    val node: SkillNodeEntity,
    val isUnlocked: Boolean,
    val isAvailable: Boolean
)

class SkillRepository @Inject constructor(
    private val skillDao: SkillDao
) {
    fun getSkillTreeStatus(): Flow<List<SkillNodeWithStatus>> {
        return combine(
            skillDao.getAllNodes(),
            skillDao.getAllEdges(),
            skillDao.getAllUnlocks()
        ) { nodes, edges, unlocks ->
            val unlockedIds = unlocks.map { it.nodeId }.toSet()
            nodes.map { node ->
                val parents = edges.filter { it.childId == node.id }.map { it.parentId }
                val isUnlocked = unlockedIds.contains(node.id)
                val isAvailable = parents.isEmpty() || parents.all { unlockedIds.contains(it) }
                SkillNodeWithStatus(
                    node = node,
                    isUnlocked = isUnlocked,
                    isAvailable = isAvailable
                )
            }
        }
    }

    suspend fun unlockNode(nodeId: String): Boolean {
        val parents = skillDao.getParentEdges(nodeId)
        val unlocks = skillDao.getUnlockedNodes()
        val unlockedIds = unlocks.map { it.id }.toSet()

        val canUnlock = parents.isEmpty() || parents.all { unlockedIds.contains(it.parentId) }

        return if (canUnlock) {
            skillDao.unlockNode(SkillUnlockEntity(nodeId = nodeId))
            true
        } else {
            false
        }
    }

    suspend fun getActiveXpMultiplier(): Float {
        val xpMultNodes = skillDao.getUnlockedNodesByType(SkillType.XP_MULT)
        return xpMultNodes.fold(1.0f) { acc, node -> acc * node.value }
    }

    suspend fun hasUnlockedPerk(type: SkillType): Boolean {
        return skillDao.getUnlockedNodesByType(type).isNotEmpty()
    }
}