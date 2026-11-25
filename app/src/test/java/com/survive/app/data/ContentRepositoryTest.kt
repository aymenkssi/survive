package com.survive.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Vérifie l'intégrité du contenu embarqué : chaque ajout de fiche doit passer ces tests.
 * Lancer : ./gradlew test
 */
class ContentRepositoryTest {

    private val repo = ContentRepository { path ->
        requireNotNull(javaClass.classLoader!!.getResource(path)) { "Fichier manquant : $path" }.readText()
    }

    /** Sources autorisées : doivent être décrites dans docs/SOURCES.md. */
    private val knownSources: Set<String> by lazy {
        val doc = listOf(File("../docs/SOURCES.md"), File("docs/SOURCES.md")).first { it.exists() }
        Regex("^\\| `([^`]+)`", RegexOption.MULTILINE).findAll(doc.readText()).map { it.groupValues[1] }.toSet()
    }

    @Test fun everyCategoryHasGuides() {
        assertTrue(repo.categories.isNotEmpty())
        repo.categories.forEach { assertTrue("Catégorie vide : ${it.id}", repo.guidesFor(it.id).isNotEmpty()) }
    }

    @Test fun idsAreUnique() {
        assertEquals(repo.guides.size, repo.guides.map { it.id }.toSet().size)
        assertEquals(repo.categories.size, repo.categories.map { it.id }.toSet().size)
    }

    @Test fun guidesAreComplete() {
        val catIds = repo.categories.map { it.id }.toSet()
        repo.guides.forEach { g ->
            assertTrue("${g.id} : catégorie inconnue", g.categoryId in catIds)
            assertTrue("${g.id} : aucune étape", g.steps.isNotEmpty())
            assertTrue("${g.id} : aucune source", g.sources.isNotEmpty())
            assertTrue("${g.id} : id invalide", g.id.matches(Regex("[a-z0-9-]+")))
        }
    }

    @Test fun sourcesAreDocumented() {
        repo.guides.flatMap { g -> g.sources.map { g.id to it } }.forEach { (id, s) ->
            assertTrue("$id : source « $s » absente de docs/SOURCES.md", s in knownSources)
        }
    }

    @Test fun searchIgnoresAccentsAndCase() {
        val ids = repo.search("BRULURE").map { it.id }
        assertTrue("brulure" in ids)
        assertTrue(repo.search("hemorragie").any { it.id == "hemorragie" })
        assertTrue(repo.search("x").isEmpty())
    }

    @Test fun toolLinksAreValid() {
        repo.guides.mapNotNull { g -> g.tool?.let { g.id to it } }.forEach { (id, tool) ->
            assertTrue("$id : outil inconnu « $tool »", tool in Tools.all)
        }
        assertEquals("cpr", repo.guide("massage-cardiaque")?.tool)
    }

    @Test fun criticalGuidesExist() {
        assertTrue(repo.criticalGuides().any { it.id == "massage-cardiaque" })
    }
}
