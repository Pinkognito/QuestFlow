package com.example.questflow.data.database.entity

import androidx.room.Entity

@Entity(
    tableName = "skill_edges",
    primaryKeys = ["parentId", "childId"]
)
data class SkillEdgeEntity(
    val parentId: String,
    val childId: String
)