package com.safemode.safekeepingforffx.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.safemode.safekeepingforffx.data.reference.Caution
import com.safemode.safekeepingforffx.domain.ChecklistItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    onCheckedChange: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    trackProgress: Boolean = true
) {
    // Reference-only lists have nothing to tick, so the row takes no clicks at all rather than
    // looking interactive and doing nothing.
    val interaction = if (trackProgress) {
        // The whole row toggles, not just the checkbox - a wall of tiny tap targets is the
        // main usability failure mode for lists this long. Long-press opens the screenshot.
        Modifier.combinedClickable(
            role = Role.Checkbox,
            onClick = { onCheckedChange(!item.isChecked) },
            onLongClick = if (item.imageRes != null) onLongPress else null,
            onLongClickLabel = "Show location screenshot"
        )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(interaction)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (trackProgress) {
            Checkbox(checked = item.isChecked, onCheckedChange = null)
        }
        Column(modifier = Modifier.padding(start = if (trackProgress) 16.dp else 0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                )
                item.tag?.let {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(it)
                    }
                }
                if (item.imageRes != null) {
                    // A quiet affordance so the long-press is discoverable per row, not just from
                    // the hint at the top of the list.
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(16.dp)
                    )
                }
                item.caution?.let { CautionBadge(it) }
            }
            // Reference lists such as the item list have no place to go and nothing to find, so
            // their rows leave these blank rather than rendering an empty line.
            if (item.location.isNotBlank()) {
                Text(
                    text = item.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (item.detail.isNotBlank()) {
                Text(
                    text = item.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            (item.caution as? Caution.Guarded)?.let { guarded ->
                Text(
                    // Phrased to sidestep verb agreement - "Dark Magus Sisters" is plural,
                    // every other Dark Aeon is singular.
                    text = "Guarded by ${guarded.aeon} once you leave Bevelle. Still obtainable, " +
                        "but expect to need endgame stats first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Red for gone-forever, amber for merely-guarded. Using one colour for both would tell the player
 * a Dark Aeon is as final as a destroyed Home, which is the opposite of true.
 */
@Composable
private fun CautionBadge(caution: Caution) {
    val container = when (caution) {
        Caution.Missable -> MaterialTheme.colorScheme.error
        is Caution.Guarded -> MaterialTheme.colorScheme.tertiary
    }
    val content = when (caution) {
        Caution.Missable -> MaterialTheme.colorScheme.onError
        is Caution.Guarded -> MaterialTheme.colorScheme.onTertiary
    }
    Badge(
        containerColor = container,
        contentColor = content,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(caution.label)
    }
}
