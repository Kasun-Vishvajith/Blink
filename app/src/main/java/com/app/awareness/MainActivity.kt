package com.app.awareness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.app.awareness.ui.theme.BlinkTheme

import com.app.awareness.ui.BlinkNavGraph
import com.app.awareness.ui.Routes
import android.content.Context

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlinkTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val prefs = getSharedPreferences("awareness_settings", Context.MODE_PRIVATE)
                    val start = if (prefs.getBoolean("onboarding_complete", false)) Routes.HOME else Routes.ONBOARDING
                    
                    BlinkNavGraph(startDestination = start)
                }
            }
        }
    }
}
