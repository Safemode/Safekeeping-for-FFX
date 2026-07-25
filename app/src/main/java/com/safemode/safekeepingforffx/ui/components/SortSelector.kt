package com.safemode.safekeepingforffx.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Fits inside a 48dp header row, so a picker costs the list no vertical space. */
val COMPACT_PILL_HEIGHT = 40.dp

/**
 * One way of ordering a list, as the picker needs to describe it: [label] on the pill, [description]
 * under it in the menu. Implemented by each screen's own order enum, since what the orders *are* is
 * a fact about that list and not something to share.
 */
interface SortOption {
    val label: String

    /** A line saying what the order actually does. Labels alone rarely manage it. */
    val description: String
}

/**
 * The order picker: a pill showing what you are looking at, tapped to swap it. Built like the Sphere
 * Grid's grid picker so the two read as the same control, and shared across screens so a list that
 * gains a second order doesn't invent a third way of offering it.
 *
 * Sized to sit inside a header row without stretching it, which is why the pill carries only the
 * label. The list underneath removes any doubt anyway - its headers say which order you are in at a
 * glance.
 */
@Composable
fun <T : SortOption> SortSelector(
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { menu = true },
            contentPadding = PaddingValues(start = 12.dp, end = 4.dp),
            modifier = Modifier.heightIn(max = COMPACT_PILL_HEIGHT)
        ) {
            Text(selected.label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Change the order of this list")
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("${option.label} order")
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(option)
                        menu = false
                    }
                )
            }
        }
    }
}
