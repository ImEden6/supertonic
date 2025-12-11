package com.supertone.supertonic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.supertone.supertonic.ui.screens.BatchScreen
import com.supertone.supertonic.ui.screens.SettingsScreen
import com.supertone.supertonic.ui.screens.TTSScreen
import com.supertone.supertonic.ui.theme.SupertonicTheme
import com.supertone.supertonic.ui.theme.ThemePalette
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object TTS : Screen("tts")
    object Settings : Screen("settings")
    object Batch : Screen("batch")
}

class MainActivity : ComponentActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        preferencesManager = PreferencesManager(this)
        
        setContent {
            val themePalette by preferencesManager.themePalette.collectAsState(initial = ThemePalette.PURPLE)
            val isDarkModePref by preferencesManager.isDarkMode.collectAsState(initial = false)
            val defaultVoice by preferencesManager.defaultVoice.collectAsState(initial = "M1")
            val systemDarkMode = isSystemInDarkTheme()
            val scope = rememberCoroutineScope()
            
            SupertonicTheme(
                palette = themePalette,
                darkTheme = isDarkModePref || systemDarkMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = Screen.TTS.route
                    ) {
                        composable(Screen.TTS.route) {
                            TTSScreen(
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                onNavigateToBatch = {
                                    navController.navigate(Screen.Batch.route)
                                }
                            )
                        }
                        
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                currentPalette = themePalette,
                                isDarkMode = isDarkModePref,
                                defaultVoice = defaultVoice,
                                onPaletteChange = { palette ->
                                    scope.launch {
                                        preferencesManager.setThemePalette(palette)
                                    }
                                },
                                onDarkModeChange = { enabled ->
                                    scope.launch {
                                        preferencesManager.setDarkMode(enabled)
                                    }
                                },
                                onDefaultVoiceChange = { voice ->
                                    scope.launch {
                                        preferencesManager.setDefaultVoice(voice)
                                    }
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(Screen.Batch.route) {
                            BatchScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
