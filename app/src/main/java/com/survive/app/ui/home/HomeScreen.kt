package com.survive.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.data.Category
import com.survive.app.data.ContentRepository
import com.survive.app.data.UserPrefs
import com.survive.app.ui.components.ToolInfo
import com.survive.app.ui.components.categoryIcon
import com.survive.app.ui.components.parseColor
import com.survive.app.ui.components.toolInfos
import com.survive.app.ui.theme.LocalNightRed
import com.survive.app.ui.theme.accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ContentRepository,
    prefs: UserPrefs,
    onCategory: (String) -> Unit,
    onGuide: (String) -> Unit,
    onTool: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onSupport: () -> Unit,
) {
    val categories = remember { repository.categories }
    val critical = remember { repository.criticalGuides() }
    val favoriteIds by prefs.favorites.collectAsState()
    val favorites = remember(favoriteIds) { favoriteIds.mapNotNull(repository::guide).sortedBy { it.title } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Survive", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Rechercher") }
                    IconButton(onClick = onSupport) { Icon(Icons.Filled.FavoriteBorder, contentDescription = "Soutenir l'application") }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Réglages") }
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
            fullWidth { OfflineBanner() }

            fullWidth { SectionTitle("Outils hors ligne") }
            items(toolInfos, key = { "tool-" + it.id }) { tool -> ToolTile(tool) { onTool(tool.id) } }

            if (critical.isNotEmpty()) {
                fullWidth { SectionTitle("Urgence – accès rapide") }
                fullWidth { Chips(critical.map { it.id to it.title }, onGuide, urgent = true) }
            }
            if (favorites.isNotEmpty()) {
                fullWidth { SectionTitle("Mes favoris", Icons.Filled.Star) }
                fullWidth { Chips(favorites.map { it.id to it.title }, onGuide, urgent = false) }
            }
            fullWidth { SectionTitle("Thématiques") }
            items(categories, key = { it.id }) { cat ->
                CategoryTile(cat, repository.guidesFor(cat.id).size) { onCategory(cat.id) }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun SectionTitle(text: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Icon(it, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun OfflineBanner() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text(
                "Tout le contenu est disponible hors ligne. En cas d'urgence vitale, appelez le 112 : " +
                    "l'appel passe par n'importe quel réseau mobile disponible.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Rangée défilant horizontalement : garde l'accueil compact même avec beaucoup de fiches. */
@Composable
private fun Chips(items: List<Pair<String, String>>, onClick: (String) -> Unit, urgent: Boolean) {
    val urgentColor = if (LocalNightRed.current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.first }) { (id, title) ->
            AssistChip(
                onClick = { onClick(id) },
                label = { Text(title) },
                colors = if (urgent) AssistChipDefaults.assistChipColors(labelColor = urgentColor)
                else AssistChipDefaults.assistChipColors(),
            )
        }
    }
}

@Composable
private fun ToolTile(tool: ToolInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(tool.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(tool.subtitle, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CategoryTile(category: Category, count: Int, onClick: () -> Unit) {
    val color = accent(parseColor(category.color))
    Card(
        modifier = Modifier.fillMaxWidth().height(130.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.18f)),
    ) {
        Column(Modifier.padding(14.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(categoryIcon(category.icon), contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Column {
                Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$count fiches", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
