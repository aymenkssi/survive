package com.survive.app.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.tools.Beeper
import com.survive.app.tools.Morse
import com.survive.app.tools.Torch
import com.survive.app.tools.playPulses
import com.survive.app.ui.components.BackTopBar
import com.survive.app.ui.theme.LocalNightRed
import kotlinx.coroutines.delay

private enum class LightMode { OFF, ON, SOS, STROBE }

/** Lampe torche : flash continu, SOS automatique, stroboscope, ou écran blanc. */
@Composable
fun TorchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val torch = remember { Torch(context) }
    val beeper = remember { Beeper() }
    var mode by remember { mutableStateOf(LightMode.OFF) }
    var withSound by remember { mutableStateOf(false) }
    var screenLight by remember { mutableStateOf(!torch.available) }
    var lit by remember { mutableStateOf(false) }

    KeepScreenOn()
    MaxBrightness(screenLight && mode != LightMode.OFF)
    DisposableEffect(Unit) { onDispose { torch.set(false); beeper.release() } }

    val output: (Boolean) -> Unit = { on ->
        lit = on
        if (!screenLight) torch.set(on)
    }

    LaunchedEffect(mode, screenLight, withSound) {
        torch.set(false)
        when (mode) {
            LightMode.OFF -> output(false)
            LightMode.ON -> output(true)
            LightMode.SOS -> while (true) {
                playPulses(Morse.toPulses(Morse.SOS), 200, output, if (withSound) beeper else null)
                delay(1400) // pause de 7 unités entre deux SOS
            }
            LightMode.STROBE -> while (true) {
                output(true); delay(80); output(false); delay(420)
            }
        }
    }

    // Plein écran lumineux quand l'écran sert de lampe
    if (screenLight && mode != LightMode.OFF) {
        val lightColor = if (LocalNightRed.current) Color.Red else Color.White
        Box(
            Modifier.fillMaxSize().background(if (lit) lightColor else Color.Black)
                .clickable { mode = LightMode.OFF },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text("Toucher pour arrêter", color = Color.Gray, modifier = Modifier.padding(32.dp))
        }
        return
    }

    Scaffold(topBar = { BackTopBar("Lampe & SOS", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!torch.available) {
                Text("Pas de flash détecté : l'écran sera utilisé comme lampe.", color = MaterialTheme.colorScheme.error)
            }
            BigButton("Lampe", mode == LightMode.ON) { mode = if (mode == LightMode.ON) LightMode.OFF else LightMode.ON }
            BigButton("SOS automatique", mode == LightMode.SOS) { mode = if (mode == LightMode.SOS) LightMode.OFF else LightMode.SOS }
            BigButton("Stroboscope", mode == LightMode.STROBE) { mode = if (mode == LightMode.STROBE) LightMode.OFF else LightMode.STROBE }

            HorizontalDivider()
            SwitchRow("Utiliser l'écran au lieu du flash", screenLight, enabled = torch.available) { screenLight = it }
            SwitchRow("Bip sonore avec le SOS", withSound) { withSound = it }
            Text(
                "Le SOS (· · · — — — · · ·) est compris partout dans le monde. La nuit, un flash se voit à plusieurs kilomètres. " +
                    "Le stroboscope consomme moins de batterie qu'une lumière continue.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun BigButton(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = if (active) ButtonDefaults.buttonColors()
    else ButtonDefaults.outlinedButtonColors()
    Button(
        onClick = onClick,
        colors = colors,
        border = if (active) null else ButtonDefaults.outlinedButtonBorder(true),
        modifier = Modifier.fillMaxWidth().height(72.dp),
    ) {
        Text(if (active) "$label : ACTIF" else label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
