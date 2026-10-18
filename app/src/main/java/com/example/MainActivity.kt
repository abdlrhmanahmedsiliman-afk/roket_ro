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
import com.example.service.AuthManager
import com.example.service.FavoritesLiveWorker
import com.example.service.NotificationCenter
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainShellScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.AppColors
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var sportsRepository: SportsRepository
    private lateinit var geminiAiService: GeminiAiService
    private lateinit var authManager: AuthManager
    private lateinit var notificationCenter: NotificationCenter
    private lateinit var favoritesWorker: FavoritesLiveWorker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = RoketRoDatabase.getDatabase(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        sportsRepository = SportsRepository(database.matchDao(), database.rosterDao(), preferencesManager, lifecycleScope, applicationContext)
        geminiAiService = GeminiAiService()
        authManager = AuthManager(applicationContext)
        notificationCenter = NotificationCenter()

        // Start background live match and goals tracking
        favoritesWorker = FavoritesLiveWorker(sportsRepository, preferencesManager, notificationCenter, lifecycleScope)
        favoritesWorker.startTracking()

        setContent {
            MyApplicationTheme {
                // RTL layout direction by default for Arabic UI
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AppColors.Bg
                    ) {
                        var showSplash by remember { mutableStateOf(true) }
                        val userProfile by authManager.userProfile.collectAsState()
                        var showLogin by remember { mutableStateOf(!userProfile.isOnboarded) }

                        Crossfade(targetState = showSplash to showLogin, label = "main_nav_transition") { (isSplash, isLogin) ->
                            when {
                                isSplash -> {
                                    SplashScreen(
                                        onSplashFinished = { showSplash = false }
                                    )
                                }
                                isLogin -> {
                                    LoginScreen(
                                        authManager = authManager,
                                        onLoginSuccess = { showLogin = false }
                                    )
                                }
                                else -> {
                                    MainShellScreen(
                                        repository = sportsRepository,
                                        preferencesManager = preferencesManager,
                                        aiService = geminiAiService,
                                        authManager = authManager,
                                        notificationCenter = notificationCenter
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
