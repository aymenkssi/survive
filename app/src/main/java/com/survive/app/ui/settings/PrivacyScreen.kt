package com.survive.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.R
import com.survive.app.ui.components.BackTopBar
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PrivacySection(val title: String, val body: String)

@Serializable
data class PrivacyText(val title: String, val intro: String, val sections: List<PrivacySection>)

@Serializable
data class PrivacyPolicy(val updated: String, val updatedEn: String, val contact: String, val fr: PrivacyText, val en: PrivacyText)

/** Lit la politique de confidentialité embarquée (même source que la page en ligne docs/confidentialite.html). */
fun loadPrivacyPolicy(read: (String) -> String): PrivacyPolicy =
    Json { ignoreUnknownKeys = true }.decodeFromString(read("privacy/privacy.json"))

/** Politique de confidentialité, lisible hors ligne dans la langue de l'application. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lang = stringResource(R.string.lang_code)
    val policy = remember {
        loadPrivacyPolicy { path -> context.assets.open(path).bufferedReader().use { it.readText() } }
    }
    val text = if (lang == "fr") policy.fr else policy.en
    val updated = if (lang == "fr") policy.updated else policy.updatedEn

    Scaffold(topBar = { BackTopBar(text.title, onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.privacy_updated, updated), style = MaterialTheme.typography.labelMedium)
            Text(text.intro, style = MaterialTheme.typography.bodyLarge)
            text.sections.forEach { s ->
                Text(s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp))
                Text(s.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
