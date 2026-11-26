package com.survive.app.ui.maps

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.R
import com.survive.app.data.maps.LocalMap
import com.survive.app.data.maps.MapCatalog
import com.survive.app.data.maps.MapDownload
import com.survive.app.data.maps.MapStore
import com.survive.app.data.maps.formatBytes
import com.survive.app.ui.components.BackTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun sizeUnits(): List<String> = stringResource(R.string.size_units).split(",")

/** Accueil des cartes hors ligne : mes cartes, téléchargements en cours, import, coordonnées GPS. */
@Composable
fun MapsScreen(store: MapStore, onCatalog: () -> Unit, onOpen: (String) -> Unit, onBack: () -> Unit) {
    var maps by remember { mutableStateOf<List<LocalMap>>(emptyList()) }
    var downloads by remember { mutableStateOf<List<MapDownload>>(emptyList()) }
    var free by remember { mutableLongStateOf(0L) }
    var importing by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<LocalMap?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val units = sizeUnits()
    val importError = stringResource(R.string.import_error)
    val importDone = stringResource(R.string.import_done, "%s")

    // Rafraîchit la liste et la progression chaque seconde tant que l'écran est affiché.
    LaunchedEffect(refresh) {
        while (true) {
            val (d, m, f) = withContext(Dispatchers.IO) { Triple(store.downloads(), store.localMaps(), store.freeBytes()) }
            downloads = d; maps = m; free = f
            delay(1000)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            val result = withContext(Dispatchers.IO) { runCatching { store.import(uri) }.getOrNull() }
            importing = false
            refresh++
            snackbar.showSnackbar(if (result == null) importError else importDone.format(result.name))
        }
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.tool_maps), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(stringResource(R.string.maps_intro), style = MaterialTheme.typography.bodyMedium) }
            item {
                Button(onClick = onCatalog, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Filled.Download, null); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.maps_download), fontWeight = FontWeight.Bold)
                }
            }
            item {
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) }, enabled = !importing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.FileOpen, null); Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (importing) R.string.importing else R.string.maps_import))
                }
            }

            if (downloads.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.maps_downloading)) }
                items(downloads, key = { "dl-" + it.fileName }) { d ->
                    DownloadRow(d, units) { store.cancel(d); refresh++ }
                }
            }

            item { SectionTitle(stringResource(R.string.maps_mine)) }
            if (maps.isEmpty()) {
                item { Text(stringResource(R.string.maps_none), style = MaterialTheme.typography.bodyMedium) }
            }
            items(maps, key = { it.file.name }) { map ->
                MapRow(map, units, onOpen = { onOpen(map.file.name) }, onDelete = { toDelete = map })
            }
            item {
                Text(stringResource(R.string.free_space, formatBytes(free, units)), style = MaterialTheme.typography.labelMedium)
            }
            item { GpsCoordinatesCard() }
            item {
                Text(MapCatalog.ATTRIBUTION, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }

    toDelete?.let { map ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_confirm, map.name)) },
            confirmButton = {
                TextButton(onClick = { store.delete(map); toDelete = null; refresh++ }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun DownloadRow(d: MapDownload, units: List<String>, onCancel: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(d.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            }
            if (d.total > 0) LinearProgressIndicator(progress = { d.progress }, modifier = Modifier.fillMaxWidth())
            else LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                if (!d.running && d.downloaded == 0L) stringResource(R.string.maps_waiting)
                else stringResource(R.string.maps_progress, formatBytes(d.downloaded, units),
                    if (d.total > 0) formatBytes(d.total, units) else "?"),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun MapRow(map: LocalMap, units: List<String>, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Map, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(map.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(map.region, formatBytes(map.sizeBytes, units)).filter { it.isNotEmpty() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) }
        }
    }
}
