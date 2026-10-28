package com.example.yogaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.yogaapp.ui.YogaAppRoot
import com.example.yogaapp.ui.theme.YogaAppTheme
import com.example.yogaapp.update.PlayAppUpdater
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Uses Android 12+ system splash via androidx.core:splashscreen
 * (compatible with minSdk 26 via the SplashScreen compat API). Compose is installed via
 * [setContent]; status bar styling is handled by [enableEdgeToEdge].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { /* Play closes the update screen itself. The app keeps running if the user declines. */ }

    private val playUpdater by lazy { PlayAppUpdater(this, updateLauncher) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb(),
            ),
        )
        setContent {
            YogaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    YogaAppRoot()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        playUpdater.check()
    }
}
