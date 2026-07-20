package com.safemode.safekeepingforffx.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.safemode.safekeepingforffx.domain.ChecklistItem

/**
 * Screenshot of where an item sits in-game, opened by long-pressing its row.
 *
 * The image is a bundled drawable, so this works offline and cannot fail to load.
 *
 * Nothing reaches this at the moment: no item carries an `imageRes`, because the screenshots were
 * pulled out. The plumbing is kept so restoring them is a data change rather than a rewrite - see
 * `for-later/RESTORING-SCREENSHOTS.md`. Note there is deliberately no attribution line any more;
 * whatever replaces the old images will need its own.
 */
@Composable
fun ScreenshotDialog(
    item: ChecklistItem,
    onDismiss: () -> Unit
) {
    val imageRes = item.imageRes ?: return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Image(
                    painter = painterResource(imageRes),
                    contentDescription = "Screenshot showing the location of ${item.title}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
