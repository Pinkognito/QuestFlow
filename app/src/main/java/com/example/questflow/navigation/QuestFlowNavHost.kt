package com.example.questflow.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.questflow.presentation.AppViewModel
import com.example.questflow.presentation.screens.today.TodayScreen
import com.example.questflow.presentation.screens.calendar.CalendarXpScreen
import com.example.questflow.presentation.screens.collection.CollectionScreen
import com.example.questflow.presentation.screens.collection.CollectionManageScreen
import com.example.questflow.presentation.screens.skilltree.SkillTreeManagementScreen
import com.example.questflow.presentation.screens.categories.CategoriesScreen

@Composable
fun QuestFlowNavHost(
    navController: NavHostController,
    appViewModel: AppViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "today"
    ) {
        composable("today") {
            TodayScreen(
                navController = navController,
                appViewModel = appViewModel
            )
        }
        composable("calendar_xp") {
            CalendarXpScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("collection") {
            CollectionScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("collection_manage") {
            CollectionManageScreen(
                navController = navController,
                appViewModel = appViewModel
            )
        }
        composable("skill_tree") {
            SkillTreeManagementScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable("categories") {
            CategoriesScreen(
                appViewModel = appViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}