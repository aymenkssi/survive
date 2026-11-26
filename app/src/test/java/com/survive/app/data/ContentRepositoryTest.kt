package com.survive.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Vérifie l'intégrité du contenu embarqué, dans chaque langue, et la cohérence entre les langues.
 * Lancer : ./gradlew test
 */
class ContentRepositoryTest {

    private fun repo(lang: String) = ContentRepository({ path ->
        requireNotNull(javaClass.classLoader!!.getResource(path)) { "Fichier manquant : $path" }.readText()
    }, lang)

    private val langs = listOf("fr", "en")
    private val fr = repo("fr")
    private val en = repo("en")

    /** Sources autorisées : doivent être décrites dans docs/SOURCES.md. */
    private val knownSources: Set<String> by lazy {
        val doc = listOf(File("../docs/SOURCES.md"), File("docs/SOURCES.md")).first { it.exists() }
        Regex("^\\| `([^`]+)`", RegexOption.MULTILINE).findAll(doc.readText()).map { it.groupValues[1] }.toSet()
    }

    @Test fun everyCategoryHasGuides() = langs.map(::repo).forEach { repo ->
        assertTrue(repo.categories.isNotEmpty())
        repo.categories.forEach { assertTrue("[${repo.lang}] catégorie vide : ${it.id}", repo.guidesFor(it.id).isNotEmpty()) }
    }

    @Test fun idsAreUnique() = langs.map(::repo).forEach { repo ->
        assertEquals(repo.guides.size, repo.guides.map { it.id }.toSet().size)
        assertEquals(repo.categories.size, repo.categories.map { it.id }.toSet().size)
    }

    @Test fun guidesAreComplete() = langs.map(::repo).forEach { repo ->
        val catIds = repo.categories.map { it.id }.toSet()
        repo.guides.forEach { g ->
            val tag = "[${repo.lang}] ${g.id}"
            assertTrue("$tag : catégorie inconnue", g.categoryId in catIds)
            assertTrue("$tag : aucune étape", g.steps.isNotEmpty())
            assertTrue("$tag : aucune source", g.sources.isNotEmpty())
            assertTrue("$tag : titre vide", g.title.isNotBlank())
            assertTrue("$tag : id invalide", g.id.matches(Regex("[a-z0-9-]+")))
        }
    }

    /** Mêmes fiches, mêmes réglages et même nombre d'éléments : favoris et cases cochées restent valables d'une langue à l'autre. */
    @Test fun languagesHaveTheSameStructure() {
        assertEquals(fr.categories.map { it.id }, en.categories.map { it.id })
        assertEquals(fr.guides.map { it.id }.sorted(), en.guides.map { it.id }.sorted())
        fr.guides.forEach { f ->
            val e = en.guide(f.id)!!
            val tag = f.id
            assertEquals("$tag catégorie", f.categoryId, e.categoryId)
            assertEquals("$tag priorité", f.priority, e.priority)
            assertEquals("$tag sources", f.sources, e.sources)
            assertEquals("$tag outil", f.tool, e.tool)
            assertEquals("$tag checklist", f.checklist, e.checklist)
            assertEquals("$tag étapes", f.steps.size, e.steps.size)
            assertEquals("$tag avertissements", f.warnings.size, e.warnings.size)
            assertEquals("$tag astuces", f.tips.size, e.tips.size)
            assertEquals("$tag matériel", f.materials.size, e.materials.size)
            assertTrue("$tag : texte anglais identique au français", f.summary != e.summary)
        }
    }

    @Test fun sourcesAreDocumented() {
        fr.guides.flatMap { g -> g.sources.map { g.id to it } }.forEach { (id, s) ->
            assertTrue("$id : source « $s » absente de docs/SOURCES.md", s in knownSources)
        }
    }

    @Test fun toolLinksAreValid() {
        fr.guides.mapNotNull { g -> g.tool?.let { g.id to it } }.forEach { (id, tool) ->
            assertTrue("$id : outil inconnu « $tool »", tool in Tools.all)
        }
        assertEquals("cpr", fr.guide("massage-cardiaque")?.tool)
    }

    @Test fun searchIgnoresAccentsAndCase() {
        assertTrue(fr.search("BRULURE").any { it.id == "brulure" })
        assertTrue(fr.search("hemorragie").any { it.id == "hemorragie" })
        assertTrue(fr.search("x").isEmpty())
    }

    @Test fun searchWorksInEnglish() {
        assertTrue(en.search("burn").any { it.id == "brulure" })
        assertTrue(en.search("CPR").any { it.id == "massage-cardiaque" })
        assertTrue(en.search("bleeding").any { it.id == "hemorragie" })
    }

    @Test fun criticalGuidesExist() {
        assertTrue(fr.criticalGuides().any { it.id == "massage-cardiaque" })
        assertEquals(fr.criticalGuides().map { it.id }, en.criticalGuides().map { it.id })
    }
}
