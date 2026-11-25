package com.survive.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.survive.app.data.ContentRepository
import com.survive.app.data.ThemeMode
import com.survive.app.data.UserPrefs
import com.survive.app.ui.guide.GuideScreen
import com.survive.app.ui.home.HomeScreen
import com.survive.app.ui.support.SupportScreen
import com.survive.app.ui.theme.SurviveTheme
import com.survive.app.ui.tools.CompassScreen
import com.survive.app.ui.tools.CprScreen
import com.survive.app.ui.tools.MorseScreen
import com.survive.app.ui.tools.TorchScreen
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures d'écran générées sur JVM (Robolectric), sans émulateur.
 * Enregistrer : ./gradlew recordRoborazziDebug  -> app/build/outputs/roborazzi/
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val repo = ContentRepository { path -> javaClass.classLoader!!.getResource(path)!!.readText() }
    private val prefs by lazy { UserPrefs(ApplicationProvider.getApplicationContext()) }

    private fun shot(name: String, mode: ThemeMode = ThemeMode.DARK, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { SurviveTheme(mode = mode) { content() } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onRoot() =
        onNode(androidx.compose.ui.test.isRoot())

    @Test fun home() = shot("home") { HomeScreen(repo, prefs, {}, {}, {}, {}, {}, {}) }
    @Test fun homeNightRed() = shot("home_nuit_rouge", ThemeMode.NIGHT_RED) { HomeScreen(repo, prefs, {}, {}, {}, {}, {}, {}) }
    @Test fun homeLight() = shot("home_clair", ThemeMode.LIGHT) { HomeScreen(repo, prefs, {}, {}, {}, {}, {}, {}) }
    @Test fun guideCpr() = shot("fiche_massage") { GuideScreen(repo, prefs, "massage-cardiaque", {}, {}) }
    @Test fun guideChecklist() = shot("fiche_kit") { GuideScreen(repo, prefs, "kit-72h", {}, {}) }
    @Test fun torch() = shot("outil_lampe") { TorchScreen {} }
    @Test fun morse() = shot("outil_morse") { MorseScreen {} }
    @Test fun compass() = shot("outil_boussole") { CompassScreen {} }
    @Test fun cpr() = shot("outil_massage") { CprScreen {} }
    @Test fun morseListen() {
        org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        compose.setContent { SurviveTheme(mode = ThemeMode.DARK) { MorseScreen {} } }
        compose.onNode(androidx.compose.ui.test.hasText("Écouter")).performClick()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/outil_morse_ecouter.png")
    }

    @Test fun morseTranslate() {
        compose.setContent { SurviveTheme(mode = ThemeMode.DARK) { MorseScreen {} } }
        compose.onNode(androidx.compose.ui.test.hasText("Traduire")).performClick()
        compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("... --- ... / .- .. -.. .")
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/outil_morse_traduire.png")
    }

    @Test fun support() = shot("soutenir") { SupportScreen {} }
}
