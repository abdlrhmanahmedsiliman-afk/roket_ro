package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.example.data.local.RoketRoDatabase
import com.example.service.GeminiAiService
import com.example.service.PreferencesManager
import com.example.service.SportsRepository
import com.example.ui.screens.MainShellScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.AppColors
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var sportsRepository: SportsRepository
    private lateinit var geminiAiService: GeminiAiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = RoketRoDatabase.getDatabase(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        sportsRepository = SportsRepository(database.matchDao(), database.rosterDao(), preferencesManager, lifecycleScope, applicationContext)
        geminiAiService = GeminiAiService()

        setContent {
            MyApplicationTheme {
                // RTL layout direction by default for Arabic UI
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AppColors.Bg
                    ) {
                        var showSplash by remember { mutableStateOf(true) }

                        Crossfade(targetState = showSplash, label = "splash_transition") { isSplash ->
                            if (isSplash) {
                                SplashScreen(
                                    onSplashFinished = { showSplash = false }
                                )
                            } else {
                                MainShellScreen(
                                    repository = sportsRepository,
                                    preferencesManager = preferencesManager,
                                    aiService = geminiAiService
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
