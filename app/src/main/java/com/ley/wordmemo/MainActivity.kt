package com.ley.wordmemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ley.wordmemo.ui.navigation.AppNavHost
import com.ley.wordmemo.ui.theme.WordMemoTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.ley.wordmemo.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            WordMemoTheme(
                darkMode = settings.darkMode,
                customPrimary = settings.primaryColor,
                customSecondary = settings.secondaryColor,
            ) {
                AppNavHost(navController = rememberNavController())
            }
        }
    }
}