package com.survive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.survive.app.ui.SurviveNavHost
import com.survive.app.ui.theme.SurviveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SurviveApp
        setContent {
            val theme by app.prefs.theme.collectAsState()
            val largeText by app.prefs.largeText.collectAsState()
            SurviveTheme(mode = theme, largeText = largeText) {
                SurviveNavHost(app.repository, app.prefs)
            }
        }
    }
}
