package com.safemode.safekeepingforffx.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Material's minimum touch target, and so the height of an [IconButton]. */
private val ACTION_SLOT = 48.dp

@Composable
fun ChecklistProgressHeader(
    foundCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$foundCount / $totalCount found",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            // The slot is always laid out, so the button appearing on the first check doesn't
            // shove the count and progress bar down. Hidden at zero: a destructive control
            // shouldn't sit there offering to undo nothing.
            if (onReset != null) {
                Box(
                    modifier = Modifier.size(ACTION_SLOT),
                    contentAlignment = Alignment.Center
                ) {
                    if (foundCount > 0) {
                        IconButton(onClick = onReset) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = "Reset this list",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
        LinearProgressIndicator(
            progress = { if (totalCount == 0) 0f else foundCount.toFloat() / totalCount },
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp, top = 8.dp)
        )
    }
}
