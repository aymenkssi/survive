package com.survive.app.ui

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.survive.app.R
import com.survive.app.data.ContentRepository
import com.survive.app.data.ContentSource
import com.survive.app.data.ThemeMode
import com.survive.app.data.UserPrefs
import com.survive.app.data.maps.MapStore
import com.survive.app.ui.guide.GuideScreen
import com.survive.app.ui.home.HomeScreen
import com.survive.app.ui.maps.MapViewerScreen
import com.survive.app.ui.maps.MapsScreen
import com.survive.app.ui.theme.SurviveTheme
import com.survive.app.ui.tools.CprScreen
import com.survive.app.ui.tools.MorseScreen
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.RandomAccessFile

/**
 * Captures brutes pour la fiche Google Play (1080 × 1920), dans chaque langue.
 * Ne s'exécute que sur demande, voir play-store/README.md (« Refaire les images ») :
 *   STORE_RAW_DIR=$PWD/play-store/build/raw STORE_MAP=/chemin/africa__tunisia.map \
 *     ./gradlew testDebugUnitTest --tests '*StoreImagesTest*'
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi", sdk = [34])
class StoreImagesTest(private val lang: String) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun langs() = listOf(arrayOf("fr"), arrayOf("en"))
    }

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val outDir = System.getenv("STORE_RAW_DIR")?.let { File(it, lang) }
    private val source = ContentSource { javaClass.classLoader!!.getResource(it)!!.readText() }
    private val repo by lazy { ContentRepository(source, lang) }
    private val prefs by lazy { UserPrefs(app) }
    private val store by lazy { MapStore(app) }

    /**
     * Dessine toute la fenêtre directement, comme le ferait l'écran. Utilisé pour la carte : le moteur Mapsforge
     * dessine ses tuiles dans sa propre mémoire d'image, que la capture standard ne voit pas dans Robolectric.
     */
    private fun drawWindow(name: String) {
        val root = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        repeat(30) {
            Thread.sleep(400)
            shadowOf(Looper.getMainLooper()).idle()
            compose.mainClock.advanceTimeBy(50)
            root.draw(Canvas(bmp))
        }
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shot(name: String, mode: ThemeMode = ThemeMode.DARK, after: () -> Unit = {}, content: @androidx.compose.runtime.Composable () -> Unit) {
        assumeTrue("STORE_RAW_DIR non défini", outDir != null)
        outDir!!.mkdirs()
        compose.mainClock.autoAdvance = false
        compose.setContent { ProvideLanguage(lang) { SurviveTheme(mode = mode) { content() } } }
        compose.mainClock.advanceTimeBy(1500)
        after()
        compose.onNode(isRoot()).captureRoboImage(File(outDir, "$name.png").path)
    }

    private fun installMaps(): String? {
        val real = System.getenv("STORE_MAP")?.let(::File)?.takeIf { it.exists() }
        real?.copyTo(File(store.dir, real.name), overwrite = true)
        // Deuxième carte pour la liste (fichier creux : seule la taille compte ici).
        RandomAccessFile(File(store.dir, "europe__france__ile-de-france.map"), "rw").use { it.setLength(201L shl 20) }
        return real?.name
    }

    @Test fun s01_home() = shot("01-home") { HomeScreen(repo, prefs, {}, {}, {}, {}, {}, {}) }

    @Test fun s02_mapViewer() {
        val name = installMaps()
        assumeTrue("STORE_MAP non défini", name != null)
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val lm = app.getSystemService(LocationManager::class.java)
        shadowOf(lm).setLocationEnabled(true)
        shadowOf(lm).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shot("02-map", after = {
            shadowOf(lm).simulateLocation(Location(LocationManager.GPS_PROVIDER).apply {
                latitude = 36.80003; longitude = 10.18083; accuracy = 6f; time = System.currentTimeMillis()
            })
            compose.mainClock.advanceTimeBy(500)
        }) { MapViewerScreen(store, name!!, {}) }
        drawWindow("02-map")
    }

    @Test fun s03_guide() = shot("03-guide") { GuideScreen(repo, prefs, "massage-cardiaque", {}, {}) }

    @Test fun s04_cpr() = shot("04-cpr") { CprScreen {} }

    @Test fun s05_morse() = shot("05-morse", after = {
        compose.onNode(hasText(if (lang == "fr") "Traduire" else "Translate")).performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNode(hasSetTextAction()).performTextInput("... --- ... / .- .. -.. .")
        compose.mainClock.advanceTimeBy(500)
    }) { MorseScreen {} }

    @Test fun s06_maps() {
        installMaps()
        shot("06-maps") { MapsScreen(store, {}, {}, {}) }
    }

    @Test fun s07_checklist() {
        if (prefs.checked.value.none { it.startsWith("kit-72h#") }) listOf(0, 1, 2, 4).forEach { prefs.toggleChecked("kit-72h", it) }
        shot("07-checklist") { GuideScreen(repo, prefs, "kit-72h", {}, {}) }
    }

    @Test fun s08_nightRed() = shot("08-night", ThemeMode.NIGHT_RED) { HomeScreen(repo, prefs, {}, {}, {}, {}, {}, {}) }

    /** Icône 512 × 512 de la fiche : l'icône de lancement, sans masque (Google Play applique le sien). */
    @Test fun s09_icon() {
        assumeTrue("STORE_RAW_DIR non défini", outDir != null)
        if (lang != "fr") return
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(ContextCompat.getColor(app, R.color.ic_launcher_background))
        // Le premier plan adaptatif fait 108 dp dont 72 dp visibles : on garde ces proportions.
        // Agrandi de 25 % : sur la fiche, l'icône n'est pas rognée comme sur l'écran d'accueil.
        ContextCompat.getDrawable(app, R.drawable.ic_launcher_foreground)!!.apply { setBounds(-64, -64, size + 64, size + 64) }.draw(canvas)
        File(outDir!!.parentFile, "icon-512.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
