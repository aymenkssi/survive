package com.survive.app.ui.maps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.R
import com.survive.app.data.ContentRepository
import com.survive.app.data.maps.CatalogEntry
import com.survive.app.data.maps.MapCatalog
import com.survive.app.data.maps.MapStore
import com.survive.app.data.maps.formatBytes
import com.survive.app.ui.components.BackTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface CatalogState {
    data object Loading : CatalogState
    data object Error : CatalogState
    data class Loaded(val entries: List<CatalogEntry>) : CatalogState
}

/** Parcours du catalogue en ligne : continents -> pays -> régions, et lancement des téléchargements. */
@Composable
fun CatalogScreen(
    store: MapStore,
    folder: String,
    onFolder: (String) -> Unit,
    onBack: () -> Unit,
    loader: suspend (String) -> List<CatalogEntry> = { withContext(Dispatchers.IO) { MapCatalog.fetch(it) } },
) {
    var state by remember(folder) { mutableStateOf<CatalogState>(CatalogState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    var filter by remember(folder) { mutableStateOf("") }
    var tick by remember { mutableIntStateOf(0) } // relit l'état des fichiers après un téléchargement
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val units = sizeUnits()
    val started = stringResource(R.string.download_started, "%s")
    val noSpace = stringResource(R.string.not_enough_space, "%s")

    LaunchedEffect(folder, attempt) {
        state = CatalogState.Loading
        state = runCatching { loader(folder) }.fold({ CatalogState.Loaded(it) }, { CatalogState.Error })
    }

    val title = if (folder.isEmpty()) stringResource(R.string.maps_download)
    else folder.trimEnd('/').split('/').joinToString(" › ") { MapCatalog.prettyName(it) }

    Scaffold(topBar = { BackTopBar(title, onBack) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            when (val s = state) {
                CatalogState.Loading -> Row(Modifier.padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.catalog_loading))
                }
                CatalogState.Error -> Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.catalog_error), color = MaterialTheme.colorScheme.error)
                    Button(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                }
                is CatalogState.Loaded -> {
                    if (folder.isEmpty()) {
                        Text(stringResource(R.string.catalog_hint), style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                    if (s.entries.size > 12) {
                        OutlinedTextField(
                            value = filter, onValueChange = { filter = it }, singleLine = true,
                            label = { Text(stringResource(R.string.filter)) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                    }
                    val q = ContentRepository.normalize(filter)
                    val shown = s.entries.filter { q.isEmpty() || q in ContentRepository.normalize(it.displayName) }
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(shown, key = { it.path }) { entry ->
                            key(tick) {
                                EntryRow(entry, store, units,
                                    onFolder = { onFolder(entry.path) },
                                    onDownload = {
                                        val need = entry.approxBytes
                                        if (need != null && need + (50L shl 20) > store.freeBytes()) {
                                            scope.launch { snackbar.showSnackbar(noSpace.format(formatBytes(need, units))) }
                                        } else {
                                            store.download(entry, "Survivor – ${entry.displayName}")
                                            tick++
                                            scope.launch { snackbar.showSnackbar(started.format(entry.displayName)) }
                                        }
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: CatalogEntry, store: MapStore, units: List<String>,
    onFolder: () -> Unit, onDownload: () -> Unit,
) {
    if (entry.isFolder) {
        Card(onClick = onFolder, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(12.dp))
                Text(entry.displayName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }
        return
    }
    val downloading = store.isDownloading(entry.localFileName)
    val present = !downloading && store.file(entry.localFileName).exists()
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Map, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                entry.approxBytes?.let { Text(formatBytes(it, units), style = MaterialTheme.typography.labelMedium) }
            }
            when {
                downloading -> Text(stringResource(R.string.in_progress), style = MaterialTheme.typography.labelLarge)
                present -> Text(stringResource(R.string.downloaded), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                else -> FilledTonalButton(onClick = onDownload) { Text(stringResource(R.string.download)) }
            }
        }
    }
}
