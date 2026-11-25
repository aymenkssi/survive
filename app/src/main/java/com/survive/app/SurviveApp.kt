package com.survive.app

import android.app.Application
import com.survive.app.data.AssetContentSource
import com.survive.app.data.ContentRepository
import com.survive.app.data.UserPrefs

/** Point d'entrée : expose les dépendances uniques (injection manuelle, sans framework). */
class SurviveApp : Application() {
    val repository: ContentRepository by lazy { ContentRepository(AssetContentSource(assets)) }
    val prefs: UserPrefs by lazy { UserPrefs(this) }
}
