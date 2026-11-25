package com.survive.app.ui.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.data.ContentRepository
import com.survive.app.ui.components.BackTopBar
import com.survive.app.ui.components.PriorityBadge

@Composable
fun GuideScreen(repository: ContentRepository, guideId: String, onBack: () -> Unit) {
    val guide = remember(guideId) { repository.guide(guideId) }

    Scaffold(topBar = { BackTopBar(guide?.title ?: "Fiche introuvable", onBack) }) { padding ->
        if (guide == null) return@Scaffold
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PriorityBadge(guide.priority)
            Text(guide.summary, style = MaterialTheme.typography.bodyLarge)

            if (guide.warnings.isNotEmpty()) {
                Surface(color = Color(0x33D32F2F), shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(12.dp).fillMaxWidth()) {
                        Text("⚠ Attention", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        guide.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }

            if (guide.materials.isNotEmpty()) {
                Section("Matériel")
                guide.materials.forEach { Text("• $it") }
            }

            Section("Étapes")
            guide.steps.forEachIndexed { i, step -> StepRow(i + 1, step) }

            if (guide.tips.isNotEmpty()) {
                Section("Astuces")
                guide.tips.forEach { Text("💡 $it") }
            }

            if (guide.sources.isNotEmpty()) {
                Text(
                    "Sources : " + guide.sources.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun StepRow(n: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("$n", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
