package com.survive.app.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.tools.Beeper
import com.survive.app.tools.Morse
import com.survive.app.tools.Torch
import com.survive.app.tools.playPulses
import com.survive.app.ui.components.BackTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Traduit un texte en Morse et l'émet par le flash, le son et un voyant à l'écran. */
@Composable
fun MorseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val torch = remember { Torch(context) }
    val beeper = remember { Beeper() }
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("SOS") }
    var useTorch by remember { mutableStateOf(torch.available) }
    var useSound by remember { mutableStateOf(true) }
    var slow by remember { mutableStateOf(true) }
    var lit by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var playing by remember { mutableStateOf(false) }
    val code = remember(text) { Morse.encode(text) }

    KeepScreenOn()
    DisposableEffect(Unit) { onDispose { job?.cancel(); torch.set(false); beeper.release() } }

    Scaffold(topBar = { BackTopBar("Morse", onBack) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(76.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(padding),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it.take(80) },
                        label = { Text("Message") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        code.ifEmpty { "—" }.replace(".", "•").replace("-", "—"),
                        fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineSmall,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).background(
                                if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape,
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                        Button(
                            enabled = code.isNotEmpty(),
                            onClick = {
                                if (playing) { job?.cancel(); return@Button }
                                job = scope.launch {
                                    playing = true
                                    try {
                                        playPulses(
                                            Morse.toPulses(code), if (slow) 250 else 120,
                                            onChange = { on -> lit = on; if (useTorch) torch.set(on) },
                                            beeper = if (useSound) beeper else null,
                                        )
                                    } finally {
                                        playing = false
                                        torch.set(false)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) { Text(if (playing) "Arrêter" else "Émettre", fontWeight = FontWeight.Bold) }
                    }
                    SwitchRow("Flash", useTorch, enabled = torch.available) { useTorch = it }
                    SwitchRow("Son", useSound) { useSound = it }
                    SwitchRow("Vitesse lente (plus lisible de loin)", slow) { slow = it }
                    Text("Alphabet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            items(Morse.table.entries.toList()) { (c, m) ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$c", fontWeight = FontWeight.Bold)
                        Text(m.replace(".", "•").replace("-", "—"), fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
