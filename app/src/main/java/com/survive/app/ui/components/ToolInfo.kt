package com.survive.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.ui.graphics.vector.ImageVector
import com.survive.app.data.Tools

data class ToolInfo(val id: String, val title: String, val subtitle: String, val icon: ImageVector)

val toolInfos = listOf(
    ToolInfo(Tools.TORCH, "Lampe & SOS", "Flash, SOS, stroboscope", Icons.Filled.FlashlightOn),
    ToolInfo(Tools.MORSE, "Morse", "Émettre un message", Icons.Filled.GraphicEq),
    ToolInfo(Tools.COMPASS, "Boussole", "Trouver le nord", Icons.Filled.Explore),
    ToolInfo(Tools.CPR, "Massage cardiaque", "Métronome 110/min", Icons.Filled.FavoriteBorder),
)

fun toolInfo(id: String): ToolInfo? = toolInfos.firstOrNull { it.id == id }
