package com.survive.app.ui

import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.survive.app.data.ThemeMode
import com.survive.app.data.maps.CatalogEntry
import com.survive.app.data.maps.MapStore
import com.survive.app.ui.maps.CatalogScreen
import com.survive.app.ui.maps.MapViewerScreen
import com.survive.app.ui.maps.MapsScreen
import com.survive.app.ui.theme.SurviveTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Captures des écrans de cartes. Pour le rendu d'une vraie carte, fournir un petit fichier .map :
 *   SURVIVOR_TEST_MAP=/chemin/europe-monaco.map ./gradlew recordRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
class MapsScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val store by lazy { MapStore(ApplicationProvider.getApplicationContext()) }

    private fun installTestMap(): String? {
        val source = System.getenv("SURVIVOR_TEST_MAP")?.let(::File)?.takeIf { it.exists() } ?: return null
        source.copyTo(File(store.dir, source.name), overwrite = true)
        return source.name
    }

    private fun capture(name: String, lang: String = "fr", content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { ProvideLanguage(lang) { SurviveTheme(mode = ThemeMode.DARK) { content() } } }
        compose.mainClock.advanceTimeBy(2_000)
        compose.onNode(isRoot()).captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun mapsHome() {
        installTestMap()
        capture("cartes") { MapsScreen(store, {}, {}, {}) }
    }

    @Test fun catalog() = capture("cartes_catalogue", "en") {
        CatalogScreen(store, "africa/", {}, {}, loader = {
            listOf(
                CatalogEntry("africa/algeria.map", false, "302M"),
                CatalogEntry("africa/morocco.map", false, "214M"),
                CatalogEntry("africa/senegal-and-gambia.map", false, "58M"),
                CatalogEntry("africa/tunisia.map", false, "67M"),
            )
        })
    }

    @Test fun viewer() {
        val name = installTestMap()
        assumeTrue("SURVIVOR_TEST_MAP non défini", name != null)
        capture("cartes_affichage") { MapViewerScreen(store, name!!, {}) }
        // Laisse le rendu des tuiles (fil d'arrière-plan) se terminer, puis recapture.
        Thread.sleep(4000)
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNode(isRoot()).captureRoboImage("build/outputs/roborazzi/cartes_affichage.png")
    }
}
