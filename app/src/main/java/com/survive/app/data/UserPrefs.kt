package com.survive.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK, NIGHT_RED }

/** Langue de l'interface et du contenu. SYSTEM suit la langue du téléphone (français si FR, sinon anglais). */
enum class AppLanguage(val code: String?) { SYSTEM(null), FR("fr"), EN("en") }

/**
 * Préférences locales de l'utilisateur (favoris, thème, taille du texte, cases cochées).
 * Stockées dans SharedPreferences : rien ne quitte le téléphone.
 */
class UserPrefs(context: Context) {
    private val sp = context.getSharedPreferences("survive_prefs", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(sp.getStringSet(KEY_FAV, emptySet())!!.toSet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(sp.getString(KEY_THEME, null)!!) }.getOrDefault(ThemeMode.SYSTEM)
    )
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _language = MutableStateFlow(
        runCatching { AppLanguage.valueOf(sp.getString(KEY_LANG, null)!!) }.getOrDefault(AppLanguage.SYSTEM)
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _largeText = MutableStateFlow(sp.getBoolean(KEY_LARGE, false))
    val largeText: StateFlow<Boolean> = _largeText.asStateFlow()

    /** Cases cochées des checklists, sous la forme "guideId#indexEtape". */
    private val _checked = MutableStateFlow(sp.getStringSet(KEY_CHECKED, emptySet())!!.toSet())
    val checked: StateFlow<Set<String>> = _checked.asStateFlow()

    fun toggleFavorite(guideId: String) {
        _favorites.value = _favorites.value.toggle(guideId)
        sp.edit().putStringSet(KEY_FAV, _favorites.value).apply()
    }

    fun setTheme(mode: ThemeMode) {
        _theme.value = mode
        sp.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        sp.edit().putString(KEY_LANG, language.name).apply()
    }

    fun setLargeText(enabled: Boolean) {
        _largeText.value = enabled
        sp.edit().putBoolean(KEY_LARGE, enabled).apply()
    }

    fun toggleChecked(guideId: String, step: Int) {
        _checked.value = _checked.value.toggle("$guideId#$step")
        sp.edit().putStringSet(KEY_CHECKED, _checked.value).apply()
    }

    fun resetChecklist(guideId: String) {
        _checked.value = _checked.value.filterNot { it.startsWith("$guideId#") }.toSet()
        sp.edit().putStringSet(KEY_CHECKED, _checked.value).apply()
    }

    private fun Set<String>.toggle(v: String) = if (v in this) this - v else this + v

    private companion object {
        const val KEY_FAV = "favorites"
        const val KEY_THEME = "theme"
        const val KEY_LANG = "language"
        const val KEY_LARGE = "large_text"
        const val KEY_CHECKED = "checked"
    }
}
