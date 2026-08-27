package com.ley.wordmemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ley.wordmemo.ui.main.MainTabsScreen
import com.ley.wordmemo.ui.chat.ChatScreen
import com.ley.wordmemo.ui.study.StudyScreen

object Routes {
    const val MAIN = "main"
    const val STUDY = "study"
    const val AI = "ai?word={word}&meaning={meaning}"

    fun ai(word: String, meaning: String = "") =
        "ai?word=${java.net.URLEncoder.encode(word, "UTF-8")}&meaning=${java.net.URLEncoder.encode(meaning, "UTF-8")}"
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
            StudyScreen(
                onBack = { navController.popBackStack() },
                onAskAi = { word, meaning ->
                    navController.navigate(Routes.ai(word, meaning))
                },
            )
        }
        composable(
            route = Routes.AI,
            arguments = listOf(
                androidx.navigation.navArgument("word") { defaultValue = "" },
                androidx.navigation.navArgument("meaning") { defaultValue = "" },
            ),
        ) { entry ->
            val word = entry.arguments?.getString("word") ?: ""
            val meaning = entry.arguments?.getString("meaning") ?: ""
            ChatScreen(
                onBack = { navController.popBackStack() },
                wordContext = word,
                meaning = meaning,
            )
        }
    }
}