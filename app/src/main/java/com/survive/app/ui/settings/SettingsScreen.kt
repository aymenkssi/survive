package com.survive.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.R
import com.survive.app.data.AppLanguage
import com.survive.app.data.ThemeMode
import com.survive.app.data.UserPrefs
import com.survive.app.ui.components.BackTopBar
import com.survive.app.ui.tools.SwitchRow

@Composable
fun SettingsScreen(prefs: UserPrefs, onSupport: () -> Unit, onBack: () -> Unit, onPrivacy: () -> Unit = {}) {
    val theme by prefs.theme.collectAsState()
    val language by prefs.language.collectAsState()
    val large by prefs.largeText.collectAsState()

    Scaffold(topBar = { BackTopBar(stringResource(R.string.settings), onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Title(R.string.language)
            listOf(
                AppLanguage.SYSTEM to R.string.lang_system,
                AppLanguage.FR to R.string.lang_fr,
                AppLanguage.EN to R.string.lang_en,
            ).forEach { (lang, label) -> Choice(label, language == lang) { prefs.setLanguage(lang) } }

            HorizontalDivider()
            Title(R.string.display)
            listOf(
                ThemeMode.SYSTEM to R.string.theme_system,
                ThemeMode.LIGHT to R.string.theme_light,
                ThemeMode.DARK to R.string.theme_dark,
                ThemeMode.NIGHT_RED to R.string.theme_night,
            ).forEach { (mode, label) -> Choice(label, theme == mode) { prefs.setTheme(mode) } }
            SwitchRow(stringResource(R.string.large_text), large) { prefs.setLargeText(it) }

            HorizontalDivider()
            OutlinedButton(onClick = onSupport, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.support_button)) }
            OutlinedButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.privacy_button)) }
            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Title(@StringRes text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun Choice(@StringRes label: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(label))
    }
}
