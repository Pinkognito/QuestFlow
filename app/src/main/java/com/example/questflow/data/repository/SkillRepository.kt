package com.example.questflow.data.repository

import com.example.questflow.data.database.dao.SkillDao
import com.example.questflow.data.database.entity.SkillNodeEntity
import com.example.questflow.data.database.entity.SkillEdgeEntity
import com.example.questflow.data.database.entity.SkillEffectType
import com.example.questflow.data.database.entity.SkillUnlockEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import javax.inject.Inject

data class SkillNodeWithStatus(
    val node: SkillNodeEntity,
    val isUnlocked: Boolean,
    val isAvailable: Boolean,
    val currentInvestment: Int = 0,
    val prerequisitesInfo: String = ""
)

class SkillRepository @Inject constructor(
    val skillDao: SkillDao // Public for direct edge access in ViewModel
) {
    fun getSkillTreeStatus(categoryId: Long? = null): Flow<List<SkillNodeWithStatus>> {
        val nodesFlow = when (categoryId) {
            null -> skillDao.getGlobalNodes()
            else -> skillDao.getNodesByCategory(categoryId)
        }

        return combine(
            nodesFlow,
            skillDao.getAllEdges(),
            skillDao.getAllUnlocks()
        ) { nodes, edges, unlocks ->
            val unlocksMap = unlocks.associateBy { it.nodeId }

            nodes.map { node ->
                val parentEdges = edges.filter { it.childId == node.id }
                val unlock = unlocksMap[node.id]
                val isUnlocked = unlock != null

                // Check prerequisites
                val prerequisitesMet = parentEdges.all { edge ->
                    val parentUnlock = unlocksMap[edge.parentId]
                    parentUnlock != null && parentUnlock.investedPoints >= edge.minParentInvestment
                }

                val isAvailable = parentEdges.isEmpty() || prerequisitesMet

                // Build prerequisites info
                val prereqInfo = if (parentEdges.isNotEmpty()) {
                    parentEdges.joinToString(", ") { edge ->
                        val parentNode = nodes.find { it.id == edge.parentId }
                        val parentInvestment = unlocksMap[edge.parentId]?.investedPoints ?: 0
                        "${parentNode?.title ?: edge.parentId}: $parentInvestment/${edge.minParentInvestment}"
                    }
                } else ""

                SkillNodeWithStatus(
                    node = node,
                    isUnlocked = isUnlocked,
                    isAvailable = isAvailable,
                    currentInvestment = unlock?.investedPoints ?: 0,
                    prerequisitesInfo = prereqInfo
                )
            }
        }
    }

    // === Node CRUD ===
    suspend fun createSkillNode(node: SkillNodeEntity) {
        skillDao.insertNode(node)
    }

    suspend fun updateSkillNode(node: SkillNodeEntity) {
        skillDao.updateNode(node)
    }

    suspend fun deleteSkillNode(nodeId: String) {
        skillDao.deleteAllEdgesForNode(nodeId)
        skillDao.deleteUnlock(nodeId)
        skillDao.deleteNode(nodeId)
    }

    suspend fun getSkillNode(nodeId: String): SkillNodeEntity? {
        return skillDao.getNodeById(nodeId)
    }

    // === Edge CRUD ===
    suspend fun createSkillEdge(parentId: String, childId: String, minParentInvestment: Int = 1) {
        skillDao.insertEdge(SkillEdgeEntity(parentId, childId, minParentInvestment))
    }

    suspend fun deleteSkillEdge(parentId: String, childId: String) {
        skillDao.deleteEdge(parentId, childId)
    }

    suspend fun getParentEdges(nodeId: String): List<SkillEdgeEntity> {
        return skillDao.getParentEdges(nodeId)
    }

    suspend fun getChildEdges(nodeId: String): List<SkillEdgeEntity> {
        return skillDao.getChildEdges(nodeId)
    }

    // === Investment Logic ===
    suspend fun investSkillPoint(nodeId: String): InvestmentResult {
        val node = skillDao.getNodeById(nodeId)
            ?: return InvestmentResult(false, "Skill not found")

        val unlock = skillDao.getUnlock(nodeId)
        val currentInvestment = unlock?.investedPoints ?: 0

        // Check if already at max
        if (currentInvestment >= node.maxInvestment) {
            return InvestmentResult(false, "Skill already at maximum")
        }

        // Check prerequisites
        val parentEdges = skillDao.getParentEdges(nodeId)
        val prerequisitesMet = parentEdges.all { edge ->
            val parentUnlock = skillDao.getUnlock(edge.parentId)
            parentUnlock != null && parentUnlock.investedPoints >= edge.minParentInvestment
        }

        if (!prerequisitesMet) {
            return InvestmentResult(false, "Prerequisites not met")
        }

        // Invest point
        val newInvestment = currentInvestment + 1
        val newUnlock = SkillUnlockEntity(
            nodeId = nodeId,
            investedPoints = newInvestment,
            unlockedAt = unlock?.unlockedAt ?: LocalDateTime.now(),
            lastInvestedAt = LocalDateTime.now()
        )
        skillDao.insertOrUpdateUnlock(newUnlock)

        return InvestmentResult(
            success = true,
            newInvestment = newInvestment,
            maxInvestment = node.maxInvestment
        )
    }

    suspend fun refundSkillPoint(nodeId: String): InvestmentResult {
        val unlock = skillDao.getUnlock(nodeId)
            ?: return InvestmentResult(false, "Skill not invested")

        val currentInvestment = unlock.investedPoints

        if (currentInvestment <= 0) {
            return InvestmentResult(false, "No points invested")
        }

        // Check if any children depend on this skill
        val childEdges = skillDao.getChildEdges(nodeId)
        val hasBlockingChildren = childEdges.any { edge ->
            val childUnlock = skillDao.getUnlock(edge.childId)
            childUnlock != null && edge.minParentInvestment >= currentInvestment
        }

        if (hasBlockingChildren) {
            return InvestmentResult(false, "Cannot refund: other skills depend on this investment")
        }

        // Refund point
        val newInvestment = currentInvestment - 1
        if (newInvestment == 0) {
            skillDao.deleteUnlock(nodeId)
        } else {
            skillDao.insertOrUpdateUnlock(unlock.copy(investedPoints = newInvestment))
        }

        return InvestmentResult(
            success = true,
            newInvestment = newInvestment,
            refunded = true
        )
    }

    // === Effect Calculation ===
    suspend fun calculateTotalEffect(effectType: SkillEffectType): Float {
        val nodes = skillDao.getUnlockedNodesByEffectType(effectType)

        return nodes.sumOf { node ->
            val investment = skillDao.getInvestedPoints(node.id) ?: 0
            (node.baseValue + node.scalingPerPoint * investment).toDouble()
        }.toFloat()
    }

    suspend fun hasEffectActive(effectType: SkillEffectType): Boolean {
        return skillDao.getUnlockedNodesByEffectType(effectType).isNotEmpty()
    }

}

data class InvestmentResult(
    val success: Boolean,
    val message: String? = null,
    val newInvestment: Int? = null,
    val maxInvestment: Int? = null,
    val refunded: Boolean = false
)