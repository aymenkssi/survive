package com.survive.app.ui.tools

import androidx.compose.ui.res.stringResource
import com.survive.app.R
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.ui.components.BackTopBar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Point cardinal le plus proche ; [names] = 8 abréviations de N à NO, dans la langue de l'interface. */
fun cardinal(deg: Float, names: List<String>): String = names[(((deg % 360 + 360) % 360 + 22.5f) / 45f).toInt() % 8]

/** Cap magnétique en degrés (0 = nord), ou null si le téléphone n'a pas de magnétomètre. */
@Composable
fun rememberHeading(): State<Float?> {
    val context = LocalContext.current
    val heading = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gravity = FloatArray(3)
        val geomag = FloatArray(3)
        val matrix = FloatArray(9)
        val orientation = FloatArray(3)
        // Lissage par moyenne de sinus/cosinus (évite le saut 359° -> 0°)
        var sx = 0f
        var sy = 0f
        fun publish(azimuthRad: Float) {
            sx = sx * 0.85f + cos(azimuthRad) * 0.15f
            sy = sy * 0.85f + sin(azimuthRad) * 0.15f
            heading.value = ((Math.toDegrees(atan2(sy, sx).toDouble()).toFloat()) + 360f) % 360f
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(matrix, e.values)
                        SensorManager.getOrientation(matrix, orientation)
                        publish(orientation[0])
                    }
                    Sensor.TYPE_ACCELEROMETER -> e.values.copyInto(gravity)
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        e.values.copyInto(geomag)
                        if (SensorManager.getRotationMatrix(matrix, null, gravity, geomag)) {
                            SensorManager.getOrientation(matrix, orientation)
                            publish(orientation[0])
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (rotation != null) {
            sm.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
        } else if (accel != null && magnet != null) {
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(listener, magnet, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm.unregisterListener(listener) }
    }
    return heading
}

@Composable
fun CompassScreen(onBack: () -> Unit) {
    val heading by rememberHeading()
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val error = MaterialTheme.colorScheme.error
    val cardinals = stringResource(R.string.cardinals).split(",")
    KeepScreenOn()

    Scaffold(topBar = { BackTopBar(stringResource(R.string.tool_compass), onBack) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val h = heading
            if (h == null) {
                Text(stringResource(R.string.compass_waiting))
                return@Column
            }
            Text("${h.roundToInt()}°  ${cardinal(h, cardinals)}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
                val r = size.minDimension / 2f * 0.92f
                val c = center
                // Repère fixe : direction vers laquelle pointe le haut du téléphone
                val marker = Path().apply {
                    moveTo(c.x, c.y - r - 4); lineTo(c.x - 14, c.y - r - 30); lineTo(c.x + 14, c.y - r - 30); close()
                }
                drawPath(marker, primary)
                rotate(-h, c) {
                    drawCircle(onSurface, r, c, style = Stroke(4f))
                    for (d in 0 until 360 step 10) {
                        val long = d % 30 == 0
                        val rad = Math.toRadians(d.toDouble() - 90)
                        val inner = r - if (long) 36 else 18
                        drawLine(
                            onSurface,
                            Offset(c.x + inner * cos(rad).toFloat(), c.y + inner * sin(rad).toFloat()),
                            Offset(c.x + r * cos(rad).toFloat(), c.y + r * sin(rad).toFloat()),
                            strokeWidth = if (long) 5f else 2f,
                        )
                    }
                    // Aiguille : rouge vers le nord
                    val north = Path().apply { moveTo(c.x, c.y - r + 50); lineTo(c.x - 22, c.y); lineTo(c.x + 22, c.y); close() }
                    val south = Path().apply { moveTo(c.x, c.y + r - 50); lineTo(c.x - 22, c.y); lineTo(c.x + 22, c.y); close() }
                    drawPath(north, error)
                    drawPath(south, onSurface.copy(alpha = 0.5f))
                }
            }
            Text(
                stringResource(R.string.compass_help),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
