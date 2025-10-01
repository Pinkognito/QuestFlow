package com.example.questflow.data.database.entity

import androidx.room.Entity

@Entity(
    tableName = "skill_edges",
    primaryKeys = ["parentId", "childId"]
)
data class SkillEdgeEntity(
    val parentId: String,
    val childId: String,
    val minParentInvestment: Int = 1  // Wie viele Punkte müssen im Parent investiert sein
)