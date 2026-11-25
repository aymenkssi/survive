package com.survive.app.ui.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.survive.app.data.ContentRepository
import com.survive.app.data.UserPrefs
import com.survive.app.ui.components.PriorityBadge
import com.survive.app.ui.components.toolInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    repository: ContentRepository,
    prefs: UserPrefs,
    guideId: String,
    onTool: (String) -> Unit,
    onBack: () -> Unit,
) {
    val guide = remember(guideId) { repository.guide(guideId) }
    val favorites by prefs.favorites.collectAsState()
    val checked by prefs.checked.collectAsState()
    val isFavorite = guideId in favorites

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(guide?.title ?: "Fiche introuvable", maxLines = 2) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    if (guide != null) IconButton(onClick = { prefs.toggleFavorite(guideId) }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (guide == null) return@Scaffold
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PriorityBadge(guide.priority)
            Text(guide.summary, style = MaterialTheme.typography.bodyLarge)

            guide.tool?.let(::toolInfo)?.let { tool ->
                Button(onClick = { onTool(tool.id) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(tool.icon, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ouvrir l'outil : ${tool.title}", fontWeight = FontWeight.Bold)
                }
            }

            if (guide.warnings.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), shape = MaterialTheme.shapes.medium) {
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

            if (guide.checklist) {
                val done = guide.steps.indices.count { "$guideId#$it" in checked }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Section("Liste ($done/${guide.steps.size})")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { prefs.resetChecklist(guideId) }) { Text("Tout décocher") }
                }
                guide.steps.forEachIndexed { i, step ->
                    CheckRow(step, "$guideId#$i" in checked) { prefs.toggleChecked(guideId, i) }
                }
            } else {
                Section("Étapes")
                guide.steps.forEachIndexed { i, step -> StepRow(i + 1, step) }
            }

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
private fun CheckRow(text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (checked) 0.5f else 1f),
        )
    }
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
