package com.survive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.survive.app.ui.SurviveNavHost
import com.survive.app.ui.theme.SurviveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as SurviveApp).repository
        setContent {
            SurviveTheme {
                SurviveNavHost(repository)
            }
        }
    }
}
