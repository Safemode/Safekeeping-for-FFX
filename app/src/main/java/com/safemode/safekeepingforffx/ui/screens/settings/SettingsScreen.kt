package com.safemode.safekeepingforffx.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safemode.safekeepingforffx.BuildConfig
import com.safemode.safekeepingforffx.R
import com.safemode.safekeepingforffx.data.reference.GameVersion
import com.safemode.safekeepingforffx.data.reference.ThemePreference
import com.safemode.safekeepingforffx.ui.screens.spheregrid.FULL_EDITOR_LABEL

private const val SOURCE_URL = "https://github.com/Safemode/Safekeeping-for-FFX"

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val selected by viewModel.gameVersion.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val resetConfirmed by viewModel.resetConfirmed.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    val showHelp by viewModel.showHelp.collectAsStateWithLifecycle()
    val tapActivates by viewModel.sphereGridTapActivates.collectAsStateWithLifecycle()
    val fullNodeEditor by viewModel.sphereGridFullNodeEditor.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ThemeDropdownRow(
            selected = theme,
            onSelect = { viewModel.setTheme(it) }
        )
        HorizontalDivider()

        SwitchRow(
            label = "Show help information",
            description = "The advice shown at the top of each category, such as when an item " +
                "becomes missable or how a list works.",
            checked = showHelp,
            onCheckedChange = viewModel::setShowHelp
        )
        HorizontalDivider()

        SwitchRow(
            label = "Sphere Grid: tap to activate",
            description = "On the Sphere Grid Planner, a tap activates a node for the selected " +
                "character and a long-press opens its details and editor. When off, a tap opens " +
                "the details instead.",
            checked = tapActivates,
            onCheckedChange = viewModel::setSphereGridTapActivates
        )
        HorizontalDivider()

        SwitchRow(
            label = FULL_EDITOR_LABEL,
            description = "What you can rewrite a node into. Off offers only the spheres a max-stats " +
                "plan places - HP +300, MP +40 and the +4 attributes - and leaves ability nodes " +
                "activate-only, so Skill, Special, White Magic and Black Magic keep the abilities " +
                "the game put there. On adds every other attribute value, all 85 abilities, and " +
                "lets ability nodes be overwritten.",
            checked = fullNodeEditor,
            onCheckedChange = viewModel::setSphereGridFullNodeEditor
        )
        HorizontalDivider()

        Text(
            text = "Game version",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
        )
        Text(
            text = "Dark Aeons were added in the International release. On the original NA PS2 " +
                "release they don't exist, so the guarded-area warnings are hidden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .selectableGroup()
        ) {
            GameVersion.entries.forEach { version ->
                ChoiceRow(
                    label = version.label,
                    description = version.description,
                    selected = version == selected,
                    onSelect = { viewModel.setGameVersion(version) }
                )
                HorizontalDivider()
            }
        }

        Text(
            text = "Items that are permanently missable - the Home primers and the Bevelle " +
                "primer - stay flagged in both versions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )

        HorizontalDivider()

        Text(
            text = "Progress",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
        )
        Text(
            text = "Unchecks every item in every list - useful when starting a new playthrough. " +
                "Your game version choice is kept.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Button(
            onClick = { showResetDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
        ) {
            Text("Reset all progress")
        }

        HorizontalDivider()

        AboutSection()
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all progress?") },
            text = {
                Text(
                    "Every item in every list will be unchecked. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetProgress()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (resetConfirmed) {
        AlertDialog(
            onDismissRequest = { viewModel.acknowledgeReset() },
            title = { Text("Progress reset") },
            text = { Text("Every list is back to zero.") },
            confirmButton = {
                TextButton(onClick = { viewModel.acknowledgeReset() }) { Text("OK") }
            }
        )
    }
}

/**
 * Everything here is read from BuildConfig rather than typed in, so bumping versionName in
 * app/build.gradle.kts is the only edit needed to change what the app reports.
 */
@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    val version = buildString {
        append(BuildConfig.VERSION_NAME)
        append(" (build ")
        append(BuildConfig.VERSION_CODE)
        append(")")
        if (BuildConfig.DEBUG) append(" debug")
    }

    Text(
        text = "About",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )

    InfoRow(label = "App", value = stringResource(R.string.app_name))
    InfoRow(label = "Version", value = version)
    InfoRow(label = "Developer", value = "Safemode")

    LinkRow(
        label = "Source code",
        value = "github.com/Safemode/Safekeeping-for-FFX",
        onClick = { uriHandler.openUri(SOURCE_URL) }
    )
    LinkRow(
        label = "Report an issue",
        value = "Open a GitHub issue",
        onClick = { uriHandler.openUri("$SOURCE_URL/issues") }
    )
    LinkRow(
        label = "Standard grid data",
        value = "theroymind/ffx-helper",
        onClick = { uriHandler.openUri("https://github.com/theroymind/ffx-helper") }
    )
    LinkRow(
        label = "Expert grid data",
        value = "Grayfox96/FFX-Sphere-Grid-viewer",
        onClick = { uriHandler.openUri("https://github.com/Grayfox96/FFX-Sphere-Grid-viewer") }
    )

    Text(
        text = "Checklist and reference data is compiled from the official guide and from " +
            "community references. The Standard Sphere Grid layout is adapted from the " +
            "theroymind/ffx-helper project; the Expert Sphere Grid is from the game-extracted data " +
            "in Grayfox96/FFX-Sphere-Grid-viewer (MIT). Corrections are welcome through the issue " +
            "tracker.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
    )
    Text(
        text = "Final Fantasy X is a trademark of Square Enix Holdings Co., Ltd. This is an " +
            "unofficial fan-made tracker and is not affiliated with or endorsed by Square Enix.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LinkRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * One row instead of four. The selected option's description stays visible underneath, so the
 * useful part (what "Midnight" actually does) isn't hidden behind the menu.
 */
@Composable
private fun ThemeDropdownRow(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(text = "Theme", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = selected.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = selected.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ThemePreference.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingIcon = {
                        if (option == selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The whole row toggles, not just the switch - the same tap-target rule the lists follow. */
@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
