package com.safemode.safekeepingforffx.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawerContent(
    currentRoute: String?,
    onDestinationClick: (FfxDestination) -> Unit
) {
    ModalDrawerSheet {
        Text(
            text = "Safekeeping for FFX",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
        )
        HorizontalDivider()

        // Takes the available space and scrolls when the screen is too short to show every
        // destination, so nothing is clipped on low-resolution devices. On tall screens the
        // weight still pushes Settings to the bottom, matching the previous layout.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            drawerDestinations.forEach { destination ->
                DrawerItem(destination, currentRoute, onDestinationClick)
            }
        }

        HorizontalDivider()
        DrawerItem(settingsDestination, currentRoute, onDestinationClick)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DrawerItem(
    destination: FfxDestination,
    currentRoute: String?,
    onClick: (FfxDestination) -> Unit
) {
    NavigationDrawerItem(
        label = { Text(destination.label) },
        icon = { Icon(destination.icon, contentDescription = null) },
        selected = destination.route == currentRoute,
        onClick = { onClick(destination) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
