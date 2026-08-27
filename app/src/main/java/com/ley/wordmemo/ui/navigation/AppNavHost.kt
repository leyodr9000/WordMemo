package com.ley.wordmemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ley.wordmemo.ui.main.MainTabsScreen
import com.ley.wordmemo.ui.study.StudyScreen

object Routes {
    const val MAIN = "main"
    const val STUDY = "study"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainTabsScreen(
                onOpenStudy = { navController.navigate(Routes.STUDY) },
                navController = navController,
            )
        }
        composable(Routes.STUDY) {
            StudyScreen(onBack = { navController.popBackStack() })
        }
    }
}