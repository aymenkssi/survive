package com.survive.app.ui.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.survive.app.data.ContentRepository
import com.survive.app.ui.components.BackTopBar
import com.survive.app.ui.components.GuideRow

@Composable
fun CategoryScreen(
    repository: ContentRepository,
    categoryId: String,
    onGuide: (String) -> Unit,
    onBack: () -> Unit,
) {
    val category = remember(categoryId) { repository.category(categoryId) }
    val guides = remember(categoryId) { repository.guidesFor(categoryId) }

    Scaffold(topBar = { BackTopBar(category?.title ?: "", onBack) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding),
        ) {
            category?.let {
                item { Text(it.description, style = MaterialTheme.typography.bodyMedium) }
            }
            items(guides, key = { it.id }) { g -> GuideRow(g) { onGuide(g.id) } }
        }
    }
}
