package com.survive.app.ui.maps

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.survive.app.R
import com.survive.app.data.maps.MapCatalog
import com.survive.app.data.maps.LocalMap
import com.survive.app.data.maps.MapStore
import kotlin.math.ln
import com.survive.app.ui.components.BackTopBar
import org.mapsforge.core.graphics.Color as MfColor
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.FixedPixelCircle
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes

/** Une carte ouverte et prête à afficher. */
private class OpenedMap(val view: MapView, val bounds: BoundingBox, val marker: FixedPixelCircle)

/** Affiche une carte hors ligne (rendu local Mapsforge) avec la position GPS et les coordonnées. */
@Composable
fun MapViewerScreen(store: MapStore, fileName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var follow by remember { mutableStateOf(false) }
    var asked by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<OpenedMap?>(null) }
    val requestLocation = rememberLocationPermissionRequest { ok -> asked++; follow = ok }
    val gps = key(asked) { rememberGpsState(active = true) }
    val granted = gps.permission
    val location = gps.location

    // Position sur la carte ; recentre si l'utilisateur a demandé « Ma position ».
    LaunchedEffect(location, opened, follow) {
        val map = opened ?: return@LaunchedEffect
        val l = location ?: return@LaunchedEffect
        val p = LatLong(l.latitude, l.longitude)
        map.marker.setLatLong(p)
        map.marker.isVisible = true
        map.marker.requestRedraw()
        if (follow) { map.view.setCenter(p); follow = false }
    }

    Scaffold(topBar = { BackTopBar(LocalMap(store.file(fileName)).name, onBack) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (failed) {
                Text(stringResource(R.string.map_error), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                return@Box
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).also { view ->
                        runCatching { opened = setUpMap(view, store, fileName) }.onFailure { failed = true }
                    }
                },
                onRelease = { view ->
                    view.destroyAll()
                    AndroidGraphicFactory.clearResourceMemoryCache()
                },
            )

            Column(
                Modifier.align(Alignment.CenterEnd).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SmallFloatingActionButton(onClick = { opened?.view?.model?.mapViewPosition?.zoomIn() }) {
                    Icon(Icons.Filled.Add, stringResource(R.string.zoom_in))
                }
                SmallFloatingActionButton(onClick = { opened?.view?.model?.mapViewPosition?.zoomOut() }) {
                    Icon(Icons.Filled.Remove, stringResource(R.string.zoom_out))
                }
                FloatingActionButton(onClick = { if (granted) follow = true else requestLocation() }) {
                    Icon(Icons.Filled.MyLocation, stringResource(R.string.my_position))
                }
            }

            if (granted) {
                Surface(
                    Modifier.align(Alignment.BottomStart).padding(12.dp).fillMaxWidth(0.8f),
                    shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp, shadowElevation = 4.dp,
                ) {
                    Column(Modifier.padding(12.dp)) {
                        GpsStatus(gps)
                        val l = location
                        val bounds = opened?.bounds
                        if (l != null && bounds != null && !bounds.contains(l.latitude, l.longitude)) {
                            Text(stringResource(R.string.map_outside), color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Surface(
                Modifier.align(Alignment.TopStart).padding(6.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), shape = MaterialTheme.shapes.small,
            ) {
                Text(MapCatalog.ATTRIBUTION, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp, 2.dp))
            }
        }
    }
}

/** Zoom qui montre à peu près toute la zone de la carte (un pays entier, ou une petite région en détail). */
internal fun zoomToFit(bounds: BoundingBox): Byte {
    val span = maxOf(bounds.longitudeSpan, bounds.latitudeSpan * 1.5).coerceAtLeast(1e-4)
    return (ln(360.0 / span) / ln(2.0) + 1).toInt().coerceIn(5, 16).toByte()
}

private fun setUpMap(view: MapView, store: MapStore, fileName: String): OpenedMap {
    view.isClickable = true
    view.mapScaleBar.isVisible = true
    val mapFile = MapFile(store.file(fileName))
    val tileCache = AndroidUtil.createTileCache(
        view.context, "map-$fileName", view.model.displayModel.tileSize, 1f,
        view.model.frameBufferModel.overdrawFactor,
    )
    val layer = TileRendererLayer(tileCache, mapFile, view.model.mapViewPosition, AndroidGraphicFactory.INSTANCE)
    layer.setXmlRenderTheme(MapsforgeThemes.DEFAULT)
    view.layerManager.layers.add(layer)

    // Point bleu de la position GPS, caché tant qu'aucune position n'est connue.
    val gf = AndroidGraphicFactory.INSTANCE
    val fill = gf.createPaint().apply { color = gf.createColor(220, 33, 150, 243); setStyle(Style.FILL) }
    val stroke = gf.createPaint().apply { color = gf.createColor(MfColor.WHITE); strokeWidth = 6f; setStyle(Style.STROKE) }
    val bounds = mapFile.boundingBox()
    val marker = FixedPixelCircle(bounds.centerPoint, 14f, fill, stroke).apply { isVisible = false }
    view.layerManager.layers.add(marker)

    view.setCenter(mapFile.startPosition() ?: bounds.centerPoint)
    view.setZoomLevel(maxOf(mapFile.startZoomLevel() ?: 0, zoomToFit(bounds)))
    return OpenedMap(view, bounds, marker)
}
