package com.survive.app.data

import kotlinx.serialization.json.Json
import java.text.Normalizer

/**
 * Abstraction de lecture des fichiers de contenu.
 * - Sur Android : implémentée avec AssetManager (voir AssetContentSource).
 * - En test JVM : implémentée avec le classpath.
 */
fun interface ContentSource {
    fun read(path: String): String
}

/**
 * Charge tout le contenu embarqué en mémoire (quelques centaines de Ko au plus),
 * puis répond aux requêtes sans aucun accès réseau ni disque supplémentaire.
 *
 * Organisation des fichiers (dans assets/), une arborescence par langue ("fr", "en") :
 *   content/<lang>/categories.json          -> List<Category>
 *   content/<lang>/guides/<categoryId>.json -> List<Guide>
 * Les identifiants sont les mêmes dans toutes les langues (favoris et cases cochées restent valables).
 */
class ContentRepository(private val source: ContentSource, val lang: String = "fr") {

    private val json = Json { ignoreUnknownKeys = true }

    val categories: List<Category> by lazy {
        json.decodeFromString<List<Category>>(source.read("content/$lang/categories.json"))
            .sortedBy { it.order }
    }

    val guides: List<Guide> by lazy {
        categories.flatMap { cat ->
            json.decodeFromString<List<Guide>>(source.read("content/$lang/guides/${cat.id}.json"))
        }
    }

    private val guidesById by lazy { guides.associateBy { it.id } }
    private val searchIndex by lazy { guides.associateWith { buildSearchText(it) } }

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }

    fun guide(id: String): Guide? = guidesById[id]

    fun guidesFor(categoryId: String): List<Guide> =
        guides.filter { it.categoryId == categoryId }.sortedBy { it.priority.ordinal }

    /** Fiches vitales, affichées en accès rapide sur l'écran d'accueil. */
    fun criticalGuides(): List<Guide> = guides.filter { it.priority == Priority.CRITICAL }

    /**
     * Recherche plein texte tolérante : insensible à la casse et aux accents,
     * tous les mots de la requête doivent être présents. Le titre pèse plus lourd.
     */
    fun search(query: String): List<Guide> {
        val words = normalize(query).split(' ').filter { it.length >= 2 }
        if (words.isEmpty()) return emptyList()
        return searchIndex
            .filter { (_, text) -> words.all { it in text } }
            .keys
            .sortedWith(
                compareByDescending<Guide> { g -> words.count { it in normalize(g.title) } }
                    .thenBy { it.priority.ordinal }
            )
    }

    private fun buildSearchText(g: Guide): String = normalize(
        listOf(g.title, g.summary, g.tags.joinToString(" "), g.materials.joinToString(" "),
            g.steps.joinToString(" "), g.tips.joinToString(" "), g.warnings.joinToString(" "))
            .joinToString(" ")
    )

    companion object {
        fun normalize(s: String): String =
            Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
                .replace("[^a-z0-9]+".toRegex(), " ")
                .trim()
    }
}
