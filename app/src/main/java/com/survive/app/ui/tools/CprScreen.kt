package com.survive.app.ui.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.survive.app.tools.Beeper
import com.survive.app.ui.components.BackTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Rythme recommandé par l'ERC : 100 à 120 compressions/min. On vise le milieu. */
private const val BPM = 110
private const val BEAT_MS = 60_000L / BPM

/** Métronome de massage cardiaque : bip + pulsation visuelle, compteur 30:2, chrono. */
@Composable
fun CprScreen(onBack: () -> Unit) {
    val beeper = remember { Beeper() }
    var running by remember { mutableStateOf(false) }
    var withBreaths by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }
    var breathing by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    KeepScreenOn()
    DisposableEffect(Unit) { onDispose { beeper.release() } }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val start = System.currentTimeMillis()
        launch { while (true) { elapsedSec = ((System.currentTimeMillis() - start) / 1000).toInt(); delay(250) } }
        count = 0
        while (true) {
            count++
            beeper.beep(60, highPitch = count == 30 && withBreaths)
            scope.launch { pulse.snapTo(1.25f); pulse.animateTo(1f) }
            delay(BEAT_MS)
            if (withBreaths && count >= 30) {
                breathing = true
                delay(5000) // ~2 insufflations, moins de 10 s d'interruption
                breathing = false
                count = 0
            }
        }
    }

    Scaffold(topBar = { BackTopBar("Massage cardiaque", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                listOf(
                    "1. Faites appeler le 112 et chercher un défibrillateur",
                    "2. Mains au centre de la poitrine, bras tendus",
                    "3. Appuyez à chaque bip, enfoncez de 5 à 6 cm",
                ).forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(200.dp).scale(if (running) pulse.value else 1f)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when { !running -> "$BPM/min"; breathing -> "2 souffles"; else -> "$count" },
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (running) {
                val m = elapsedSec / 60
                val s = elapsedSec % 60
                Text("Durée : %d:%02d".format(m, s), style = MaterialTheme.typography.titleLarge)
                if (elapsedSec >= 120 && elapsedSec % 120 < 10) {
                    Text("Si possible, changez de sauveteur maintenant (toutes les 2 min).", color = MaterialTheme.colorScheme.error)
                }
            }
            SwitchRow("Avec insufflations (30 compressions : 2 souffles) – si vous êtes formé", withBreaths, enabled = !running) { withBreaths = it }
            Button(onClick = { running = !running; breathing = false }, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Text(if (running) "ARRÊTER" else "DÉMARRER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                "Ne vous arrêtez pas avant l'arrivée des secours, la reprise d'une respiration normale ou l'arrivée d'un défibrillateur (suivez alors ses instructions).",
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            )
        }
    }
}
