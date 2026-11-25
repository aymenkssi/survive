package com.survive.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.data.ThemeMode
import com.survive.app.data.UserPrefs
import com.survive.app.ui.components.BackTopBar
import com.survive.app.ui.tools.SwitchRow

@Composable
fun SettingsScreen(prefs: UserPrefs, onSupport: () -> Unit, onBack: () -> Unit) {
    val theme by prefs.theme.collectAsState()
    val large by prefs.largeText.collectAsState()

    Scaffold(topBar = { BackTopBar("Réglages", onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Affichage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            listOf(
                ThemeMode.SYSTEM to "Automatique (selon le téléphone)",
                ThemeMode.LIGHT to "Clair – lecture en plein soleil",
                ThemeMode.DARK to "Sombre – économise la batterie (écrans OLED)",
                ThemeMode.NIGHT_RED to "Nuit rouge – préserve la vision nocturne, discret",
            ).forEach { (mode, label) ->
                Row(
                    Modifier.fillMaxWidth().selectable(selected = theme == mode, onClick = { prefs.setTheme(mode) }).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = theme == mode, onClick = { prefs.setTheme(mode) })
                    Spacer(Modifier.width(8.dp))
                    Text(label)
                }
            }
            HorizontalDivider()
            SwitchRow("Grand texte (+30 %)", large) { prefs.setLargeText(it) }
            HorizontalDivider()
            OutlinedButton(onClick = onSupport, modifier = Modifier.fillMaxWidth()) { Text("❤ Soutenir l'application") }
            Text(
                "Survive fonctionne sans aucune connexion. Vos favoris et réglages restent sur ce téléphone. " +
                    "Ce contenu ne remplace pas une formation aux premiers secours : en cas d'urgence, appelez le 112.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
