package com.survive.app

import android.app.Application
import com.survive.app.data.AssetContentSource
import com.survive.app.data.ContentRepository
import com.survive.app.data.UserPrefs
import java.util.concurrent.ConcurrentHashMap

/** Point d'entrée : expose les dépendances uniques (injection manuelle, sans framework). */
class SurviveApp : Application() {
    private val repositories = ConcurrentHashMap<String, ContentRepository>()

    /** Contenu dans la langue demandée ("fr" ou "en"), chargé une seule fois. */
    fun repository(lang: String): ContentRepository =
        repositories.getOrPut(lang) { ContentRepository(AssetContentSource(assets), lang) }

    val prefs: UserPrefs by lazy { UserPrefs(this) }
}
