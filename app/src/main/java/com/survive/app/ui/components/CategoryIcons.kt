package com.survive.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Associe le nom logique d'icône (dans categories.json) à une icône Material. */
fun categoryIcon(name: String): ImageVector = when (name) {
    "water" -> Icons.Filled.WaterDrop
    "fire" -> Icons.Filled.LocalFireDepartment
    "shelter" -> Icons.Filled.Cabin
    "food" -> Icons.Filled.Restaurant
    "medical" -> Icons.Filled.MedicalServices
    "navigation" -> Icons.Filled.Explore
    "communication" -> Icons.Filled.CellTower
    "energy" -> Icons.Filled.BatteryChargingFull
    "hygiene" -> Icons.Filled.CleanHands
    "security" -> Icons.Filled.Shield
    "kit" -> Icons.Filled.Backpack
    "knot" -> Icons.Filled.Gesture
    else -> Icons.Filled.Info
}

fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
