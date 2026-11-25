package com.survive.app.ui.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.survive.app.tools.Beeper
import com.survive.app.tools.Morse
import com.survive.app.tools.MorseDecoder
import com.survive.app.tools.SignalTracker
import com.survive.app.tools.ToneDetector
import com.survive.app.tools.Torch
import com.survive.app.tools.playPulses
import com.survive.app.ui.components.BackTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Affichage lisible : points et traits épais. */
private fun pretty(code: String) = code.replace(".", "•").replace("-", "—")

/** Morse : émettre (flash, son, écran), écouter au micro et traduire, ou traduire un code écrit. */
@Composable
fun MorseScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    KeepScreenOn()
    Scaffold(topBar = { BackTopBar("Morse", onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                listOf("Émettre", "Écouter", "Traduire").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> EmitTab()
                1 -> ListenTab()
                else -> TranslateTab()
            }
        }
    }
}

@Composable
private fun EmitTab() {
    val context = LocalContext.current
    val torch = remember { Torch(context) }
    val beeper = remember { Beeper() }
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("SOS") }
    var useTorch by remember { mutableStateOf(torch.available) }
    var useSound by remember { mutableStateOf(true) }
    var slow by remember { mutableStateOf(true) }
    var lit by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var playing by remember { mutableStateOf(false) }
    val code = remember(text) { Morse.encode(text) }

    DisposableEffect(Unit) { onDispose { job?.cancel(); torch.set(false); beeper.release() } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(76.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(80) },
                    label = { Text("Message") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Text(pretty(code).ifEmpty { "—" }, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.headlineSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Indicator(lit)
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
                    Text(pretty(m), fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private const val SAMPLE_RATE = 16_000
private const val FRAME = 160 // 10 ms

/** Écoute le micro et traduit en direct les bips Morse (sifflet, klaxon, radio, autre téléphone…). */
@Composable
private fun ListenTab() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    var listening by remember { mutableStateOf(false) }
    var decoder by remember { mutableStateOf(MorseDecoder()) }
    var text by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf("") }
    var toneOn by remember { mutableStateOf(false) }
    var level by remember { mutableFloatStateOf(0f) }
    var wpm by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(listening, decoder) {
        if (!listening) return@LaunchedEffect
        error = null
        val dec = decoder
        withContext(Dispatchers.Default) {
            val recorder = openRecorder()
            if (recorder == null) {
                withContext(Dispatchers.Main) { error = "Impossible d'ouvrir le micro."; listening = false }
                return@withContext
            }
            val tracker = SignalTracker(10, dec)
            val detector = ToneDetector(SAMPLE_RATE, FRAME)
            val buffer = ShortArray(FRAME)
            var frames = 0
            try {
                recorder.startRecording()
                while (isActive) {
                    val n = recorder.read(buffer, 0, FRAME)
                    if (n <= 0) continue
                    tracker.feed(detector.level(buffer, n))
                    dec.tick(tracker.currentSilenceMs)
                    if (++frames % 5 == 0) {
                        val t = dec.text; val p = dec.pending; val on = tracker.isOn
                        val l = (tracker.snr / 20f).coerceIn(0f, 1f); val w = dec.wpm
                        withContext(Dispatchers.Main) { text = t; pending = p; toneOn = on; level = l; wpm = w }
                    }
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
            }
        }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!granted) {
            Text("Pour écouter des signaux Morse, Survive a besoin du micro. Le son est analysé sur le téléphone, " +
                "en direct : rien n'est enregistré ni envoyé.")
            Button(onClick = { askPermission.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Autoriser le micro", fontWeight = FontWeight.Bold)
            }
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Indicator(toneOn)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                LinearProgressIndicator(progress = { if (listening) level else 0f }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    if (listening && wpm > 0) "Vitesse estimée : $wpm mots/min" else "Niveau du signal",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Button(onClick = { listening = !listening }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Text(if (listening) "ARRÊTER L'ÉCOUTE" else "ÉCOUTER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp).heightIn(min = 120.dp)) {
                Text("Message reçu", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text.ifEmpty { if (listening) "En attente de signaux…" else "—" },
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                )
                if (pending.isNotEmpty()) {
                    Text(pretty(pending), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        OutlinedButton(onClick = { decoder = MorseDecoder(); text = ""; pending = "" }, modifier = Modifier.fillMaxWidth()) {
            Text("Effacer")
        }
        Text(
            "Approchez le téléphone de la source du son (sifflet, klaxon, radio, autre téléphone) et limitez les bruits autour. " +
                "La vitesse est détectée automatiquement. Un « ? » signale une lettre mal reçue.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@SuppressLint("MissingPermission") // vérifiée par ListenTab avant l'appel
private fun openRecorder(): AudioRecord? = runCatching {
    val min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    val recorder = AudioRecord(
        MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT, maxOf(min, FRAME * 20 * 2),
    )
    if (recorder.state == AudioRecord.STATE_INITIALIZED) recorder else { recorder.release(); null }
}.getOrNull()

/** Saisie d'un code Morse (noté sur papier, entendu, vu…) et traduction en texte. */
@Composable
private fun TranslateTab() {
    var code by rememberSaveable { mutableStateOf("") }
    val translation = remember(code) { Morse.decode(code) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Tapez le code avec les touches ou le clavier : un espace entre les lettres, « / » entre les mots.",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = code, onValueChange = { code = it.take(400) },
            label = { Text("Code Morse") }, modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton("•", Modifier.weight(1f)) { code += "." }
            KeyButton("—", Modifier.weight(1f)) { code += "-" }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton("Lettre", Modifier.weight(1f)) { code += " " }
            KeyButton("Mot /", Modifier.weight(1f)) { code += " / " }
            KeyButton("⌫", Modifier.weight(1f)) { code = code.trimEnd().dropLast(1).let { if (it.endsWith(" /")) it.dropLast(2) else it } }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Traduction", style = MaterialTheme.typography.labelMedium)
                Text(translation.ifEmpty { "—" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedButton(onClick = { code = "" }, modifier = Modifier.fillMaxWidth()) { Text("Effacer") }
    }
}

@Composable
private fun KeyButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(64.dp)) {
        Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Indicator(on: Boolean) {
    Box(
        Modifier.size(40.dp).background(
            if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape,
        )
    )
}
