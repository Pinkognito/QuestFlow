package com.example.questflow.domain.usecase

import com.example.questflow.data.database.entity.SkillEffectType
import com.example.questflow.data.database.entity.SkillNodeEntity
import com.example.questflow.data.repository.SkillRepository
import java.util.UUID
import javax.inject.Inject

class ManageSkillNodeUseCase @Inject constructor(
    private val skillRepository: SkillRepository
) {
    suspend fun createSkill(
        title: String,
        description: String,
        effectType: SkillEffectType,
        baseValue: Float,
        scalingPerPoint: Float,
        maxInvestment: Int,
        iconName: String = "star",
        positionX: Float = 0f,
        positionY: Float = 0f,
        colorHex: String = "#FFD700",
        categoryId: Long? = null
    ): ManageSkillResult {
        if (title.isBlank()) {
            return ManageSkillResult(false, "Titel darf nicht leer sein")
        }

        if (maxInvestment < 1) {
            return ManageSkillResult(false, "Max. Investment muss mindestens 1 sein")
        }

        val node = SkillNodeEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            effectType = effectType,
            baseValue = baseValue,
            scalingPerPoint = scalingPerPoint,
            maxInvestment = maxInvestment,
            iconName = iconName,
            positionX = positionX,
            positionY = positionY,
            categoryId = categoryId,
            colorHex = colorHex
        )

        skillRepository.createSkillNode(node)
        return ManageSkillResult(true, nodeId = node.id)
    }

    suspend fun updateSkill(node: SkillNodeEntity): ManageSkillResult {
        if (node.title.isBlank()) {
            return ManageSkillResult(false, "Titel darf nicht leer sein")
        }

        if (node.maxInvestment < 1) {
            return ManageSkillResult(false, "Max. Investment muss mindestens 1 sein")
        }

        skillRepository.updateSkillNode(node)
        return ManageSkillResult(true, nodeId = node.id)
    }

    suspend fun deleteSkill(nodeId: String): ManageSkillResult {
        // Check if any skills depend on this skill
        val allEdges = mutableListOf<com.example.questflow.data.database.entity.SkillEdgeEntity>()
        skillRepository.skillDao.getAllEdges().collect { edges ->
            allEdges.addAll(edges)
        }

        val hasDependents = allEdges.any { it.parentId == nodeId }

        if (hasDependents) {
            val dependentSkills = mutableListOf<String>()
            allEdges.filter { it.parentId == nodeId }.forEach { edge ->
                val skillNode = skillRepository.getSkillNode(edge.childId)
                if (skillNode != null) {
                    dependentSkills.add(skillNode.title)
                }
            }

            return ManageSkillResult(
                success = false,
                message = "Kann nicht gelöscht werden: Folgende Skills hängen davon ab: ${dependentSkills.joinToString(", ")}"
            )
        }

        skillRepository.deleteSkillNode(nodeId)
        return ManageSkillResult(true)
    }

    suspend fun createEdge(
        parentId: String,
        childId: String,
        minParentInvestment: Int = 1
    ): ManageSkillResult {
        if (minParentInvestment < 1) {
            return ManageSkillResult(false, "Min. Investment muss mindestens 1 sein")
        }

        // Check for circular dependencies
        if (parentId == childId) {
            return ManageSkillResult(false, "Skill kann nicht von sich selbst abhängen")
        }

        skillRepository.createSkillEdge(parentId, childId, minParentInvestment)
        return ManageSkillResult(true)
    }

    suspend fun deleteEdge(parentId: String, childId: String): ManageSkillResult {
        skillRepository.deleteSkillEdge(parentId, childId)
        return ManageSkillResult(true)
    }
}

data class ManageSkillResult(
    val success: Boolean,
    val message: String? = null,
    val nodeId: String? = null
)
