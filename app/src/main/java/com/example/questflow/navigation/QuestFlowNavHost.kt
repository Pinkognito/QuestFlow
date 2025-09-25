package com.example.questflow.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.questflow.presentation.screens.today.TodayScreen
import com.example.questflow.presentation.screens.calendar.CalendarXpScreen
import com.example.questflow.presentation.screens.collection.MemeCollectionScreen
import com.example.questflow.presentation.screens.skilltree.SkillTreeScreen
import com.example.questflow.presentation.screens.categories.CategoriesScreen

@Composable
fun QuestFlowNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "today"
    ) {
        composable("today") {
            TodayScreen(navController = navController)
        }
        composable("calendar_xp") {
            CalendarXpScreen()
        }
        composable("collection") {
            MemeCollectionScreen()
        }
        composable("skill_tree") {
            SkillTreeScreen()
        }
        composable("categories") {
            CategoriesScreen(
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}