package com.survive.app.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.survive.app.ui.maps.GpsStatus
import com.survive.app.ui.maps.rememberGpsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Autorisation accordée mais interrupteur « Localisation » coupé, puis activé : la position doit arriver seule. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GpsStateTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val lm = app.getSystemService(LocationManager::class.java)

    private fun setUp(precise: Boolean) {
        shadowOf(app).grantPermissions(
            *if (precise) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            else arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        shadowOf(lm).setLocationEnabled(false)
        compose.setContent { ProvideLanguage("fr") { GpsStatus(rememberGpsState(active = true)); Text("ok") } }
        compose.onNodeWithText("Activer la localisation").assertExists()
    }

    private fun turnLocationOn(vararg providers: String) {
        shadowOf(lm).setLocationEnabled(true)
        providers.forEach { shadowOf(lm).setProviderEnabled(it, true) }
        app.sendBroadcast(Intent(LocationManager.PROVIDERS_CHANGED_ACTION))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    private fun fix(provider: String) = Location(provider).apply {
        latitude = 36.8065; longitude = 10.1815; accuracy = 8f; time = System.currentTimeMillis()
    }

    @Test fun positionAppearsOnceLocationIsTurnedOn() {
        setUp(precise = true)
        turnLocationOn(LocationManager.GPS_PROVIDER)
        compose.onNodeWithText("Activer la localisation").assertDoesNotExist()
        shadowOf(lm).simulateLocation(fix(LocationManager.GPS_PROVIDER))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        compose.onNodeWithText("36.80650, 10.18150").assertExists()
    }

    @Test fun approximateOnlyStillGivesAPosition() {
        setUp(precise = false)
        turnLocationOn(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
        shadowOf(lm).simulateLocation(fix(LocationManager.FUSED_PROVIDER))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        compose.onNodeWithText("36.80650, 10.18150").assertExists()
        compose.onNodeWithText("Seule la position approximative", substring = true).assertExists()
    }
}
