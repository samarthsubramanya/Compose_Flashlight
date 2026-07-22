package com.thingsenz.flashlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.thingsenz.flashlight.ui.FlashlightRoute
import com.thingsenz.flashlight.ui.theme.FlashlightTheme

class MainActivity : ComponentActivity() {

    private val viewModel: FlashlightViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlashlightTheme {
                FlashlightRoute(viewModel = viewModel)
            }
        }
    }
}
