package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skill_nodes")
data class SkillNodeEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val effectType: SkillEffectType,
    val baseValue: Float,
    val scalingPerPoint: Float,
    val maxInvestment: Int = 10,
    val iconName: String = "star",
    val positionX: Float = 0f,
    val positionY: Float = 0f,

    // Category binding (null = global skills)
    val categoryId: Long? = null,

    // Visual customization
    val colorHex: String = "#FFD700",

    // Legacy compatibility
    @Deprecated("Use effectType instead")
    val type: SkillType = SkillType.XP_MULT,
    @Deprecated("Use baseValue instead")
    val value: Float = 0f
)

@Deprecated("Replaced by SkillEffectType")
enum class SkillType {
    XP_MULT, STREAK_GUARD, EXTRA_MEME
}

enum class SkillEffectType {
    // XP-related
    XP_MULTIPLIER,              // Multiplikator auf alle XP-Gains (+X% pro Punkt)
    TASK_XP_BONUS,              // Bonus-XP für Task-Completion (+X% pro Punkt)
    CALENDAR_XP_BONUS,          // Bonus-XP für Kalender-Events (+X% pro Punkt)

    // Skill Points
    SKILL_POINT_GAIN,           // Zusätzliche Skillpunkte pro Level-Up (+X pro Punkt)

    // Collection-related
    RARE_COLLECTION_CHANCE,     // Höhere Chance auf seltenere Items (+X% pro Punkt)
    EXTRA_COLLECTION_UNLOCK,    // Zusätzliche Collection-Items pro Level-Up (boolean, 1 Punkt = aktiv)
    COLLECTION_SLOT_INCREASE,   // Mehr Slots für Collections (+X pro Punkt)

    // Streak-related
    STREAK_PROTECTION,          // Schutz vor Streak-Verlust (boolean, 1 Punkt = aktiv)
    STREAK_XP_MULTIPLIER,       // XP-Multiplikator basierend auf Streak (+X% pro Punkt)

    // Difficulty-specific bonuses
    TRIVIAL_TASK_BONUS,         // Extra XP für Trivial Tasks (+X% pro Punkt)
    EASY_TASK_BONUS,            // Extra XP für Easy Tasks (+X% pro Punkt)
    MEDIUM_TASK_BONUS,          // Extra XP für Medium Tasks (+X% pro Punkt)
    HARD_TASK_BONUS,            // Extra XP für Hard Tasks (+X% pro Punkt)
    EPIC_TASK_BONUS,            // Extra XP für Epic Tasks (+X% pro Punkt)

    // Category-specific
    CATEGORY_XP_BOOST           // Extra XP für aktuelle Kategorie (+X% pro Punkt)
}