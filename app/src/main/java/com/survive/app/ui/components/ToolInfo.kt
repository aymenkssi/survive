package com.survive.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import com.survive.app.R
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector
import com.survive.app.data.Tools

data class ToolInfo(val id: String, @StringRes val title: Int, @StringRes val subtitle: Int, val icon: ImageVector)

val toolInfos = listOf(
    ToolInfo(Tools.MAPS, R.string.tool_maps, R.string.tool_maps_sub, Icons.Filled.Map),
    ToolInfo(Tools.TORCH, R.string.tool_torch, R.string.tool_torch_sub, Icons.Filled.FlashlightOn),
    ToolInfo(Tools.MORSE, R.string.tool_morse, R.string.tool_morse_sub, Icons.Filled.GraphicEq),
    ToolInfo(Tools.COMPASS, R.string.tool_compass, R.string.tool_compass_sub, Icons.Filled.Explore),
    ToolInfo(Tools.CPR, R.string.tool_cpr, R.string.tool_cpr_sub, Icons.Filled.FavoriteBorder),
)

fun toolInfo(id: String): ToolInfo? = toolInfos.firstOrNull { it.id == id }
