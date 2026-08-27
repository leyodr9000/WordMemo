package com.ley.wordmemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ley.wordmemo.ui.home.HomeScreen
import com.ley.wordmemo.ui.settings.SettingsScreen
import com.ley.wordmemo.ui.study.StudyScreen
import com.ley.wordmemo.ui.importwords.ImportScreen
import com.ley.wordmemo.ui.books.BooksScreen

object Routes {
    const val HOME = "home"
    const val STUDY = "study"
    const val IMPORT = "import"
    const val SETTINGS = "settings"
    const val BOOKS = "books"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStudyClick = { navController.navigate(Routes.STUDY) },
                onImportClick = { navController.navigate(Routes.IMPORT) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onBooksClick = { navController.navigate(Routes.BOOKS) },
            )
        }
        composable(Routes.STUDY) {
            StudyScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.IMPORT) {
            ImportScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BOOKS) {
            BooksScreen(onBack = { navController.popBackStack() })
        }
    }
}