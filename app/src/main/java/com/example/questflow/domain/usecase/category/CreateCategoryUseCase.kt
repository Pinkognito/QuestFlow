package com.example.questflow.domain.usecase.category

import com.example.questflow.data.database.entity.CategoryEntity
import com.example.questflow.data.repository.CategoryRepository
import javax.inject.Inject

class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        color: String,
        emoji: String,
        levelScalingFactor: Float = 1.0f
    ): Long {
        val category = CategoryEntity(
            name = name,
            description = description,
            color = color,
            emoji = emoji,
            levelScalingFactor = levelScalingFactor
        )

        android.util.Log.d("QuestFlow_Category", "=== NEW CATEGORY CREATED ===")
        android.util.Log.d("QuestFlow_Category", "Name: ${category.name}")
        android.util.Log.d("QuestFlow_Category", "Initial XP: ${category.currentXp}")
        android.util.Log.d("QuestFlow_Category", "Initial Level: ${category.currentLevel}")
        android.util.Log.d("QuestFlow_Category", "Total XP: ${category.totalXp}")
        android.util.Log.d("QuestFlow_Category", "Level Scaling Factor: ${category.levelScalingFactor}")

        return categoryRepository.createCategory(category)
    }
}