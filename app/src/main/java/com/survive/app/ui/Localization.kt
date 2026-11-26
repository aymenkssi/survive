package com.survive.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.survive.app.data.AppLanguage
import java.util.Locale

/** Langues disponibles pour l'interface et le contenu. */
val SUPPORTED_LANGUAGES = listOf("fr", "en")

/** Langue effective : celle choisie, sinon celle du téléphone si elle est prise en charge, sinon l'anglais. */
fun resolveLanguage(choice: AppLanguage, system: Locale = Locale.getDefault()): String =
    choice.code ?: system.language.takeIf { it in SUPPORTED_LANGUAGES } ?: "en"

/** Contexte dont les ressources sont dans une autre langue, mais qui reste lié à l'activité (permissions, luminosité…). */
private class LocalizedContext(base: Context, private val res: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = res
}

/** Applique la langue choisie dans l'application à tout ce qui est en dessous (stringResource, etc.). */
@Composable
fun ProvideLanguage(lang: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val (context, config) = remember(lang, baseConfig) {
        val locale = Locale.forLanguageTag(lang)
        val config = Configuration(baseConfig).apply { setLocale(locale) }
        LocalizedContext(base, base.createConfigurationContext(config).resources) to config
    }
    CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config) {
        content()
    }
}
