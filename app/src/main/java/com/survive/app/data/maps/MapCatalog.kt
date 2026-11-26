package com.survive.app.data.maps

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Une entrée du catalogue : un dossier (continent, pays découpé en régions) ou un fichier de carte. */
data class CatalogEntry(
    /** Chemin relatif à [MapCatalog.ROOT], par ex. "africa/tunisia.map" ou "europe/france/". */
    val path: String,
    val isFolder: Boolean,
    /** Taille telle qu'affichée par le serveur ("67M", "1.2G"), null pour un dossier. */
    val sizeLabel: String? = null,
) {
    val fileName: String get() = path.trimEnd('/').substringAfterLast('/')

    /** Nom lisible : "south-africa-and-lesotho.map" -> "South Africa And Lesotho". */
    val displayName: String get() = MapCatalog.prettyName(fileName)

    /** Nom du fichier local, unique même pour deux régions homonymes : "europe__france__alsace.map". */
    val localFileName: String get() = path.replace("/", MapCatalog.SEP)

    /** Taille approximative en octets, d'après l'étiquette du serveur. */
    val approxBytes: Long? get() = MapCatalog.parseSize(sizeLabel)
}

/**
 * Catalogue des cartes Mapsforge (données OpenStreetMap, © contributeurs OpenStreetMap, licence ODbL).
 * Le serveur publie un fichier .map par pays ou par région, téléchargeable librement.
 */
object MapCatalog {
    const val ROOT = "https://download.mapsforge.org/maps/v5/"
    /** Sépare les niveaux du chemin dans le nom des fichiers locaux. */
    const val SEP = "__"
    const val ATTRIBUTION = "© OpenStreetMap contributors · Mapsforge"

    private val row = Regex(
        """<a href="([^"?/][^"]*)">[^<]*</a>\s*</td>\s*<td[^>]*>[^<]*</td>\s*<td[^>]*>\s*([^<]*?)\s*</td>""",
    )

    /** Lit une page d'index du serveur et garde les sous-dossiers et les fichiers .map. */
    fun parseIndex(html: String, folder: String): List<CatalogEntry> =
        row.findAll(html).mapNotNull { m ->
            val href = m.groupValues[1]
            val size = m.groupValues[2].takeUnless { it.isBlank() || it == "-" }
            when {
                href.endsWith("/") -> CatalogEntry(folder + href, isFolder = true)
                href.endsWith(".map") -> CatalogEntry(folder + href, isFolder = false, sizeLabel = size)
                else -> null
            }
        }.sortedWith(compareBy({ !it.isFolder }, { it.displayName })).toList()

    fun prettyName(fileName: String): String =
        fileName.removeSuffix(".map").split('-', '_').joinToString(" ") { w ->
            w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

    /** "67M" -> 67 Mio, "1.2G" -> 1,2 Gio, "850K" -> 850 Kio. */
    fun parseSize(label: String?): Long? {
        val m = Regex("""([\d.]+)\s*([KMG]?)""").matchEntire(label?.trim() ?: return null) ?: return null
        val n = m.groupValues[1].toDoubleOrNull() ?: return null
        val mult = when (m.groupValues[2]) { "K" -> 1L shl 10; "M" -> 1L shl 20; "G" -> 1L shl 30; else -> 1L }
        return (n * mult).toLong()
    }

    /** Télécharge et lit un dossier du catalogue (appel réseau bloquant : à lancer hors du thread principal). */
    fun fetch(folder: String): List<CatalogEntry> {
        val connection = URL(ROOT + folder).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("User-Agent", "SurvivorEndOfTheWorld-Android")
        try {
            if (connection.responseCode != 200) error("HTTP ${connection.responseCode}")
            return parseIndex(connection.inputStream.bufferedReader().use { it.readText() }, folder)
        } finally {
            connection.disconnect()
        }
    }
}

/** Taille lisible : 67 Mo, 1,2 Go (unités adaptées à la langue par l'appelant). */
fun formatBytes(bytes: Long, units: List<String> = listOf("B", "KB", "MB", "GB")): String {
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) { value /= 1024; unit++ }
    return when {
        unit < 2 || value >= 100 -> String.format(Locale.getDefault(), "%.0f %s", value, units[unit])
        else -> String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
    }
}
