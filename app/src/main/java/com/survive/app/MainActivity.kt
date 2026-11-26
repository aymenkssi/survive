package com.survive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import com.survive.app.ui.ProvideLanguage
import com.survive.app.ui.SurviveNavHost
import com.survive.app.ui.resolveLanguage
import com.survive.app.ui.theme.SurviveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SurviveApp
        setContent {
            val theme by app.prefs.theme.collectAsState()
            val largeText by app.prefs.largeText.collectAsState()
            val language by app.prefs.language.collectAsState()
            // Relit la langue du téléphone si elle change (mode « Automatique »).
            val systemLocale = LocalConfiguration.current.locales[0]
            val lang = resolveLanguage(language, systemLocale)
            ProvideLanguage(lang) {
                SurviveTheme(mode = theme, largeText = largeText) {
                    SurviveNavHost(app.repository(lang), app.prefs, app.mapStore)
                }
            }
        }
    }
}
