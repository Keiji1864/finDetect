package com.ghostteam.findetect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ghostteam.findetect.core.designsystem.theme.FinDetectTheme
import com.ghostteam.findetect.core.navigation.AppNavHost
import com.ghostteam.findetect.feature.auth.AuthGate

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FinDetectTheme {
                AuthGate { onLogout ->
                    AppNavHost(onLogout = onLogout)
                }
            }
        }
    }
}