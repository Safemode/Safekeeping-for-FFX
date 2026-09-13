package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.data.reference.DarkAeonsAndPenance
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.ui.navigation.drawerDestinations
import com.safemode.safekeepingforffx.ui.navigation.FfxDestination
import com.safemode.safekeepingforffx.ui.navigation.favoriteSources
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionAvailabilityTest {

    @Test
    fun `Dark Aeons and Penance is dropped from Home on the original PS2 release`() {
        val category = DarkAeonsAndPenance.category
        assertFalse(category.isAvailableOn(GameVersion.ORIGINAL_PS2))
        assertTrue(category.isAvailableOn(GameVersion.INTERNATIONAL_HD))
    }

    @Test
    fun `every other list counts on every release`() {
        favoriteSources.categories
            .filter { it.id != DarkAeonsAndPenance.CATEGORY_ID }
            .forEach { category ->
                GameVersion.entries.forEach { version ->
                    assertTrue("${category.id} vanished on $version", category.isAvailableOn(version))
                }
            }
    }

    @Test
    fun `Dark Aeons and Penance stays in the drawer whatever the release`() {
        // The drawer list is fixed at build time and never consults the version; this pins that down
        // so hiding the Home card can't quietly take the screen out of the menu too.
        assertTrue(
            drawerDestinations.any {
                it is FfxDestination.Checklist && it.category.id == DarkAeonsAndPenance.CATEGORY_ID
            }
        )
    }
}
