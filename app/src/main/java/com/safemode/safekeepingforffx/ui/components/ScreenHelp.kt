package com.safemode.safekeepingforffx.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * One tip in a screen's help: an icon and a sentence or two, such as a note about the list or how
 * to use the screen. A screen's tips are shown together in [HelpDialog], opened from the help
 * button in the top bar.
 */
data class HelpTip(val icon: ImageVector, val text: String)

/**
 * Offers [tips] as this screen's help for as long as it is on screen, so the top bar can show its
 * help button. Re-offered whenever the tips change, and withdrawn when the screen leaves. An empty
 * list offers nothing, which hides the button.
 */
@Composable
fun ScreenHelpEffect(tips: List<HelpTip>, onHelpChange: (List<HelpTip>?) -> Unit) {
    DisposableEffect(tips) {
        onHelpChange(tips.takeIf { it.isNotEmpty() })
        onDispose { onHelpChange(null) }
    }
}

/** A screen's tips, one per row with its icon, under the screen's name. */
@Composable
fun HelpDialog(title: String, tips: List<HelpTip>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                tips.forEach { tip ->
                    Row {
                        Icon(
                            imageVector = tip.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp).size(18.dp)
                        )
                        Text(
                            text = tip.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
