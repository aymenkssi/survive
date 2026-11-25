package com.survive.app.data

import android.content.res.AssetManager

class AssetContentSource(private val assets: AssetManager) : ContentSource {
    override fun read(path: String): String =
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
