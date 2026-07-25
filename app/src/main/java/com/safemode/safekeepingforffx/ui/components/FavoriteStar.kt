package com.safemode.safekeepingforffx.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Material's minimum touch target, and so the width a row gives up to carry a star. */
val FAVORITE_SLOT = 48.dp

/**
 * The one control that puts something in Favorites, shared by every list that offers it so the
 * gesture is identical wherever you meet it.
 *
 * A visible toggle rather than a swipe on purpose. Every row in this app already spends its tap and
 * its long-press on something else, and the app has no swipe gestures at all - so a hidden drag
 * would be both a new vocabulary and an invisible one. A star states that it exists.
 *
 * [itemName] only ever reaches a screen reader, where "Add to Favorites" repeated down a list of
 * thirty rows says nothing about which one is about to be starred.
 */
@Composable
fun FavoriteStar(
    isFavorite: Boolean,
    onFavoriteChange: (Boolean) -> Unit,
    itemName: String,
    modifier: Modifier = Modifier
) {
    IconToggleButton(
        checked = isFavorite,
        onCheckedChange = onFavoriteChange,
        modifier = modifier.size(FAVORITE_SLOT)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            contentDescription = if (isFavorite) {
                "Remove $itemName from Favorites"
            } else {
                "Add $itemName to Favorites"
            },
            // Filled and tinted is the whole signal that this one is starred, so it takes the
            // accent colour; an empty star stays as quiet as the other row marks.
            tint = if (isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp)
        )
    }
}
