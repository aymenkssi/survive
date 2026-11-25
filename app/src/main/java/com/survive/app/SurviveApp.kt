package com.survive.app

import android.app.Application
import com.survive.app.data.AssetContentSource
import com.survive.app.data.ContentRepository

/** Point d'entrée : expose un dépôt de contenu unique (injection manuelle, sans framework). */
class SurviveApp : Application() {
    val repository: ContentRepository by lazy { ContentRepository(AssetContentSource(assets)) }
}
