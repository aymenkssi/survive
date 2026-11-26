package com.survive.app.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
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
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.survive.app.R
import com.survive.app.tools.Coordinates

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** Position précise (GPS) autorisée. */
fun hasPreciseLocation(context: Context) = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)

/** Au moins la position approximative autorisée (Android 12+ permet de ne donner que celle-ci). */
fun hasLocationPermission(context: Context) =
    hasPreciseLocation(context) || granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

/** Service de localisation activé dans les paramètres (interrupteur « Localisation »), indépendant de l'autorisation. */
fun locationEnabled(context: Context): Boolean =
    context.getSystemService(LocationManager::class.java)?.let(LocationManagerCompat::isLocationEnabled) ?: false

/** Demande la permission de localisation ; [onResult] reçoit vrai si au moins la position approximative est accordée. */
@Composable
fun rememberLocationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        onResult(r.values.any { it })
    }
    return { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
}

/** État complet de la localisation, relu en direct. */
data class GpsState(
    val location: Location?,
    /** Autorisation accordée (précise ou approximative). */
    val permission: Boolean,
    /** Autorisation « position précise » accordée (nécessaire au GPS). */
    val precise: Boolean,
    /** Interrupteur « Localisation » du téléphone activé. */
    val serviceEnabled: Boolean,
)

/**
 * Suit l'autorisation, l'interrupteur de localisation et la position.
 * L'état est relu quand l'utilisateur revient dans l'application (après être passé par les paramètres)
 * et quand l'interrupteur « Localisation » change : les mises à jour GPS démarrent alors toutes seules.
 */
@SuppressLint("MissingPermission") // chaque fournisseur est demandé seulement si sa permission est accordée
@Composable
fun rememberGpsState(active: Boolean): GpsState {
    val context = LocalContext.current
    var check by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { check++ }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { check++ }
        }
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply {
            if (Build.VERSION.SDK_INT >= 30) addAction(LocationManager.MODE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    val permission = remember(check) { hasLocationPermission(context) }
    val precise = remember(check) { hasPreciseLocation(context) }
    val enabled = remember(check) { locationEnabled(context) }
    var location by remember { mutableStateOf<Location?>(null) }

    DisposableEffect(active, permission, precise, enabled) {
        val lm = context.getSystemService(LocationManager::class.java)
        if (!active || !permission || !enabled || lm == null) return@DisposableEffect onDispose { }

        // GPS seulement avec la position précise ; le fournisseur « fusionné » (Android 12+) et le réseau
        // fonctionnent aussi avec la position approximative.
        val providers = buildList {
            if (precise) add(LocationManager.GPS_PROVIDER)
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { p -> runCatching { lm.allProviders.contains(p) && lm.isProviderEnabled(p) }.getOrDefault(false) }

        providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }?.let { if (location == null || it.time > location!!.time) location = it }

        // Toutes les méthodes sont implémentées : elles sont abstraites avant Android 11.
        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) {
                val current = location
                // Garde la position la plus précise si plusieurs sources répondent en même temps.
                if (current == null || l.time - current.time > 10_000 || l.accuracy <= current.accuracy) location = l
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("API < 29") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        providers.forEach { runCatching { lm.requestLocationUpdates(it, 2000L, 0f, listener, Looper.getMainLooper()) } }
        onDispose { lm.removeUpdates(listener) }
    }
    return GpsState(location, permission, precise, enabled)
}

fun openLocationSettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
}

fun openAppPermissions(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }
}

/** Carte affichant les coordonnées GPS, lisibles et à dicter aux secours. */
@Composable
fun GpsCoordinatesCard(modifier: Modifier = Modifier) {
    var active by remember { mutableStateOf(false) }
    var asked by remember { mutableIntStateOf(0) }
    val state = key(asked) { rememberGpsState(active) }
    val request = rememberLocationPermissionRequest { ok -> asked++; active = ok }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.gps_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!active) {
                Text(stringResource(R.string.gps_explain), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { if (state.permission) active = true else request() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (state.permission) R.string.gps_start else R.string.gps_permission))
                }
            } else {
                GpsStatus(state)
            }
        }
    }
}

/** Coordonnées, ou ce qui empêche de les obtenir avec le bouton pour y remédier. */
@Composable
fun GpsStatus(state: GpsState, compact: Boolean = false) {
    val context = LocalContext.current
    val location = state.location
    when {
        !state.permission -> {
            Text(stringResource(R.string.gps_no_permission), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { openAppPermissions(context) }) { Text(stringResource(R.string.gps_open_permissions)) }
        }
        !state.serviceEnabled -> {
            Text(stringResource(R.string.gps_disabled), color = MaterialTheme.colorScheme.error)
            Button(onClick = { openLocationSettings(context) }) { Text(stringResource(R.string.gps_enable)) }
        }
        location != null -> {
            Text(Coordinates.decimal(location.latitude, location.longitude),
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(Coordinates.dms(location.latitude, location.longitude), fontFamily = FontFamily.Monospace,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium)
            if (location.hasAccuracy()) {
                Text(stringResource(R.string.gps_accuracy, location.accuracy.toInt()), style = MaterialTheme.typography.labelMedium)
            }
        }
        else -> Row {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.gps_searching), style = MaterialTheme.typography.bodySmall)
        }
    }
    if (state.permission && !state.precise) {
        Text(stringResource(R.string.gps_approximate), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary)
        TextButton(onClick = { openAppPermissions(context) }) { Text(stringResource(R.string.gps_open_permissions)) }
    }
}
