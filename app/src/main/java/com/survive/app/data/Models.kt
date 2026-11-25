package com.survive.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Une grande thématique de survie (Eau, Feu, Premiers secours…). */
@Serializable
data class Category(
    val id: String,
    val title: String,
    val description: String,
    /** Nom logique de l'icône, résolu côté UI (voir ui/components/CategoryIcons.kt). */
    val icon: String,
    /** Couleur d'accent au format #RRGGBB. */
    val color: String,
    val order: Int,
)

/** Niveau d'importance d'une fiche : sert au tri et à l'accès rapide « Urgence ». */
@Serializable
enum class Priority {
    @SerialName("critique") CRITICAL,
    @SerialName("important") IMPORTANT,
    @SerialName("utile") USEFUL,
}

/** Une fiche pratique : une technique ou une procédure précise. */
@Serializable
data class Guide(
    val id: String,
    val categoryId: String,
    val title: String,
    val summary: String,
    val priority: Priority = Priority.USEFUL,
    /** Matériel nécessaire (peut être vide). */
    val materials: List<String> = emptyList(),
    /** Étapes numérotées, dans l'ordre d'exécution. */
    val steps: List<String>,
    /** Dangers / erreurs à éviter : affichés en rouge. */
    val warnings: List<String> = emptyList(),
    /** Astuces complémentaires. */
    val tips: List<String> = emptyList(),
    /** Mots-clés supplémentaires pour la recherche (synonymes, termes familiers). */
    val tags: List<String> = emptyList(),
    /** Références des sources (voir docs/SOURCES.md). */
    val sources: List<String> = emptyList(),
)
