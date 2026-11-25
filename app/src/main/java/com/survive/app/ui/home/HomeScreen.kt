package com.survive.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.data.Category
import com.survive.app.data.ContentRepository
import com.survive.app.ui.components.categoryIcon
import com.survive.app.ui.components.parseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ContentRepository,
    onCategory: (String) -> Unit,
    onGuide: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val categories = remember { repository.categories }
    val critical = remember { repository.criticalGuides() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Survive", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Rechercher")
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OfflineBanner()
            }
            if (critical.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Urgence – accès rapide", style = MaterialTheme.typography.titleMedium)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FlowRowChips(critical.map { it.id to it.title }, onGuide)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Thématiques", style = MaterialTheme.typography.titleMedium)
            }
            items(categories, key = { it.id }) { cat ->
                CategoryTile(cat, repository.guidesFor(cat.id).size) { onCategory(cat.id) }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(
                "Tout le contenu est disponible hors ligne. En cas d'urgence vitale, " +
                    "appelez le 112 (fonctionne souvent même sans carte SIM ni forfait).",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(items: List<Pair<String, String>>, onClick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (id, title) ->
            AssistChip(
                onClick = { onClick(id) },
                label = { Text(title) },
                colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFFF8A80)),
            )
        }
    }
}

@Composable
private fun CategoryTile(category: Category, count: Int, onClick: () -> Unit) {
    val accent = parseColor(category.color)
    Card(
        modifier = Modifier.fillMaxWidth().height(130.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.18f)),
    ) {
        Column(Modifier.padding(14.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(categoryIcon(category.icon), contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
            Column {
                Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$count fiches", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
