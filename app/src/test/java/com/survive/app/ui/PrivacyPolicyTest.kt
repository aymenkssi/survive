package com.survive.app.ui

import com.survive.app.ui.settings.loadPrivacyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** La page en ligne (docs/confidentialite.html) doit reprendre exactement le texte affiché dans l'application. */
class PrivacyPolicyTest {
    private val policy = loadPrivacyPolicy { javaClass.classLoader!!.getResource(it)!!.readText() }
    private val page = listOf(File("../docs/confidentialite.html"), File("docs/confidentialite.html")).first { it.exists() }.readText()

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#x27;")

    @Test fun onlinePageIsUpToDate() {
        for (t in listOf(policy.fr, policy.en)) {
            val texts = listOf(t.title, t.intro) + t.sections.flatMap { listOf(it.title, it.body) }
            texts.forEach { assertTrue("Page en ligne pas à jour (lancer play-store/scripts/build_privacy.py) : ${it.take(60)}", esc(it) in page) }
        }
    }

    @Test fun bothLanguagesHaveTheSameSections() {
        assertEquals(policy.fr.sections.size, policy.en.sections.size)
    }

    /** Chaque permission du manifeste doit être expliquée dans la politique. */
    @Test fun everyPermissionIsExplained() {
        val text = (policy.fr.sections + policy.en.sections).joinToString(" ") { it.title + " " + it.body }
        listOf("micro", "microphone", "Localisation", "Location", "Internet").forEach { assertTrue(it, it in text) }
    }
}
