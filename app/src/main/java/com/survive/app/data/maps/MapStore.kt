package com.survive.app.data.maps

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/** Une carte présente sur le téléphone. */
data class LocalMap(val file: File) {
    /** "africa__tunisia.map" -> "Tunisia". */
    val name: String get() = MapCatalog.prettyName(file.name.substringAfterLast(MapCatalog.SEP))
    /** "europe__france__alsace.map" -> "Europe › France" (vide pour un fichier importé). */
    val region: String get() = file.name.split(MapCatalog.SEP).dropLast(1).joinToString(" › ") { MapCatalog.prettyName(it) }
    val sizeBytes: Long get() = file.length()
}

/** Un téléchargement en cours (géré par le gestionnaire de téléchargements d'Android). */
data class MapDownload(val fileName: String, val id: Long, val downloaded: Long, val total: Long, val running: Boolean) {
    val name: String get() = MapCatalog.prettyName(fileName.substringAfterLast(MapCatalog.SEP))
    val progress: Float get() = if (total > 0) downloaded.toFloat() / total else 0f
}

/**
 * Cartes hors ligne stockées dans le dossier privé de l'application (aucune permission de stockage nécessaire).
 * Les téléchargements passent par DownloadManager : ils continuent si l'application est fermée,
 * reprennent après une coupure réseau et affichent une notification.
 */
class MapStore(private val context: Context) {
    val dir: File = (context.getExternalFilesDir(DIR) ?: File(context.filesDir, DIR)).apply { mkdirs() }
    private val prefs = context.getSharedPreferences("map_downloads", Context.MODE_PRIVATE)
    private val manager get() = context.getSystemService(DownloadManager::class.java)

    private fun pending(): Map<String, Long> =
        prefs.all.mapNotNull { (k, v) -> (v as? Long)?.let { k to it } }.toMap()

    /** Cartes complètes (les fichiers encore en téléchargement sont exclus). */
    fun localMaps(): List<LocalMap> {
        val busy = pending().keys
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".map") && f.name !in busy }
            .orEmpty().sortedBy { it.name }.map(::LocalMap)
    }

    fun file(fileName: String): File = File(dir, fileName)

    fun isDownloading(fileName: String) = fileName in pending()

    fun freeBytes(): Long = dir.usableSpace

    /** Lance le téléchargement d'une carte du catalogue. */
    fun download(entry: CatalogEntry, title: String) {
        val name = entry.localFileName
        if (isDownloading(name)) return
        File(dir, name).delete()
        val request = DownloadManager.Request(Uri.parse(MapCatalog.ROOT + entry.path))
            .setTitle(title)
            .setDescription(MapCatalog.ATTRIBUTION)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, DIR, name)
            .addRequestHeader("User-Agent", "SurvivorEndOfTheWorld-Android")
        val id = manager.enqueue(request)
        prefs.edit().putLong(name, id).apply()
    }

    /** État des téléchargements ; range les terminés et nettoie les échecs. Renvoie ceux encore en cours. */
    fun downloads(): List<MapDownload> {
        val result = mutableListOf<MapDownload>()
        for ((name, id) in pending()) {
            val cursor = manager.query(DownloadManager.Query().setFilterById(id))
            cursor.use { c ->
                if (!c.moveToFirst()) { forget(name, deleteFile = true); return@use }
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> forget(name, deleteFile = false)
                    DownloadManager.STATUS_FAILED -> { manager.remove(id); forget(name, deleteFile = true) }
                    else -> result += MapDownload(name, id, done, total, status == DownloadManager.STATUS_RUNNING)
                }
            }
        }
        return result.sortedBy { it.fileName }
    }

    fun cancel(download: MapDownload) {
        manager.remove(download.id)
        forget(download.fileName, deleteFile = true)
    }

    fun delete(map: LocalMap) {
        map.file.delete()
    }

    /**
     * Importe un fichier .map choisi par l'utilisateur (carte copiée depuis un ordinateur, une carte SD…).
     * Renvoie la carte importée, ou null si le fichier n'est pas une carte Mapsforge.
     */
    fun import(uri: Uri): LocalMap? {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "imported.map"
        val name = displayName.replace(Regex("[^A-Za-z0-9._-]"), "-").let { if (it.endsWith(".map")) it else "$it.map" }
        val target = File(dir, name)
        val ok = resolver.openInputStream(uri)?.use { input ->
            val header = ByteArray(MAGIC.length)
            val n = input.read(header)
            if (n != MAGIC.length || String(header, Charsets.US_ASCII) != MAGIC) return@use false
            target.outputStream().use { out -> out.write(header); input.copyTo(out, 1 shl 16) }
            true
        } ?: false
        return if (ok) LocalMap(target) else null
    }

    private fun forget(name: String, deleteFile: Boolean) {
        prefs.edit().remove(name).apply()
        if (deleteFile) File(dir, name).delete()
    }

    companion object {
        const val DIR = "maps"
        /** Les fichiers Mapsforge commencent par cette signature. */
        const val MAGIC = "mapsforge binary OSM"
    }
}
