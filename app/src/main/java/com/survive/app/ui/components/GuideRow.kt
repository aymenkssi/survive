package com.survive.app.ui.components

import androidx.compose.ui.res.stringResource
import com.survive.app.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.survive.app.data.Guide
import com.survive.app.data.Priority
import com.survive.app.ui.theme.LocalNightRed

@Composable
fun PriorityBadge(priority: Priority) {
    val (label, color) = when (priority) {
        Priority.CRITICAL -> stringResource(R.string.priority_critical) to Color(0xFFD32F2F)
        Priority.IMPORTANT -> stringResource(R.string.priority_important) to Color(0xFFF57C00)
        Priority.USEFUL -> stringResource(R.string.priority_useful) to Color(0xFF558B2F)
    }
    val night = LocalNightRed.current
    Surface(color = if (night) MaterialTheme.colorScheme.primaryContainer else color, shape = MaterialTheme.shapes.small) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (night) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun GuideRow(guide: Guide, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    guide.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                PriorityBadge(guide.priority)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                guide.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
    }
}
