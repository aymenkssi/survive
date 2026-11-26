package com.survive.app.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.survive.app.R
import com.survive.app.tools.Coordinates

fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Demande la permission de localisation ; [onResult] reçoit vrai si au moins la position approximative est accordée. */
@Composable
fun rememberLocationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        onResult(r.values.any { it })
    }
    return { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
}

/** Dernière position connue puis mises à jour du GPS (qui fonctionne sans réseau), tant que [active]. */
@SuppressLint("MissingPermission") // vérifiée par hasLocationPermission
@Composable
fun rememberGpsLocation(active: Boolean): State<Location?> {
    val context = LocalContext.current
    val location = remember { mutableStateOf<Location?>(null) }
    DisposableEffect(active) {
        val lm = context.getSystemService(LocationManager::class.java)
        if (!active || lm == null || !hasLocationPermission(context)) return@DisposableEffect onDispose { }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        location.value = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        // Toutes les méthodes sont implémentées : elles sont abstraites avant Android 11.
        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) {
                val current = location.value
                // Garde la position la plus précise si deux sources répondent en même temps.
                if (current == null || l.time - current.time > 10_000 || l.accuracy <= current.accuracy) location.value = l
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("API < 29") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        providers.forEach { runCatching { lm.requestLocationUpdates(it, 2000L, 0f, listener, Looper.getMainLooper()) } }
        onDispose { lm.removeUpdates(listener) }
    }
    return location
}

fun locationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(LocationManager::class.java) ?: return false
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .any { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
}

/** Carte affichant les coordonnées GPS, lisibles et à dicter aux secours. */
@Composable
fun GpsCoordinatesCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasLocationPermission(context)) }
    var active by remember { mutableStateOf(false) }
    val request = rememberLocationPermissionRequest { ok -> granted = ok; active = ok }
    val location by rememberGpsLocation(active && granted)

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.gps_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!active) {
                Text(stringResource(R.string.gps_explain), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { if (granted) active = true else request() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (granted) R.string.gps_start else R.string.gps_permission))
                }
            } else {
                CoordinatesText(location, context)
            }
        }
    }
}

@Composable
fun CoordinatesText(location: Location?, context: Context = LocalContext.current) {
    when {
        location != null -> {
            Text(Coordinates.decimal(location.latitude, location.longitude),
                style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(Coordinates.dms(location.latitude, location.longitude), fontFamily = FontFamily.Monospace)
            if (location.hasAccuracy()) {
                Text(stringResource(R.string.gps_accuracy, location.accuracy.toInt()), style = MaterialTheme.typography.labelMedium)
            }
        }
        !locationEnabled(context) -> Text(stringResource(R.string.gps_disabled), color = MaterialTheme.colorScheme.error)
        else -> Row {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.gps_searching), style = MaterialTheme.typography.bodySmall)
        }
    }
}
