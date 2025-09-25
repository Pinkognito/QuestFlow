package com.example.questflow.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.questflow.data.database.entity.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestFlowTopBar(
    title: String,
    selectedCategory: CategoryEntity?,
    categories: List<CategoryEntity>,
    onCategorySelected: (CategoryEntity?) -> Unit,
    onManageCategoriesClick: () -> Unit,
    level: Int,
    totalXp: Long,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Dropdown on the left
                CategoryDropdown(
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onCategorySelected = onCategorySelected,
                    onManageCategoriesClick = onManageCategoriesClick,
                    modifier = Modifier.weight(0.4f)
                )

                // XP Badge on the right
                XpLevelBadge(
                    level = selectedCategory?.currentLevel ?: level,
                    xp = selectedCategory?.totalXp?.toLong() ?: totalXp,
                    modifier = Modifier.weight(0.6f),
                    isCategory = selectedCategory != null
                )
            }
        },
        navigationIcon = navigationIcon ?: {},
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = selectedCategory?.let { category ->
                try {
                    Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.2f)
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primaryContainer
                }
            } ?: MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}