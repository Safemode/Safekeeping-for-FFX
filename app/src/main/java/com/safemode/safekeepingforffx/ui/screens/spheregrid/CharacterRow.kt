package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safemode.safekeepingforffx.data.reference.GridCharacter

/** Width of each unselected character's slot: room for its circle, and the tap target around it. */
private val SLOT_WIDTH = 44.dp

/** Height of every slot, so each one is a full-size tap target. */
private val SLOT_HEIGHT = 48.dp

/** The coloured circle, and the height of the selected character's pill. */
private val BADGE_SIZE = 32.dp

/** Outline on every badge, so a pale one (Yuna's white, Wakka's yellow) still shows on a light theme. */
private val BADGE_BORDER = 1.dp

/** A heavier outline on the selected pill, so the selection reads at a glance as well as by shape. */
private val SELECTED_BORDER = 2.dp

/**
 * The characters in one row, each one tap away. The selected one is a pill in their colour with their
 * name; the rest are circles in their own colour with their initial, the same colour as their path on
 * the grid. Long-pressing a circle shows the name. All seven fit across a phone held upright or the
 * Sphere Grid Planner's side panel, and the row scrolls on anything narrower.
 *
 * Shared by the Sphere Grid Planner, its character status sheet, route replay and the per-character
 * checklists, so a character looks the same everywhere.
 */
@Composable
fun CharacterRow(
    selected: GridCharacter,
    onSelect: (GridCharacter) -> Unit,
    modifier: Modifier = Modifier,
    /** Who to offer. Route replay passes only the characters the route has a path for. */
    characters: List<GridCharacter> = GridCharacter.entries
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        characters.forEach { character ->
            val isSelected = character == selected
            val slot = Modifier
                .height(SLOT_HEIGHT)
                .selectable(
                    selected = isSelected,
                    onClick = { onSelect(character) },
                    role = Role.RadioButton
                )
                .semantics { contentDescription = character.displayName }
            if (isSelected) {
                Box(modifier = slot.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    SelectedPill(character)
                }
            } else {
                InitialCircle(character, modifier = slot)
            }
        }
    }
}

/** The selected character: a pill in their colour with their full name. */
@Composable
private fun SelectedPill(character: GridCharacter) {
    val color = character.activationColor()
    Box(
        modifier = Modifier
            .height(BADGE_SIZE)
            .background(color, CircleShape)
            .border(SELECTED_BORDER, MaterialTheme.colorScheme.onSurface, CircleShape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = character.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColorOn(color),
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

/** Any other character: a circle in their colour with their initial, named on long-press. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InitialCircle(character: GridCharacter, modifier: Modifier) {
    val color = character.activationColor()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(character.displayName) } },
        state = rememberTooltipState()
    ) {
        Box(modifier = modifier.size(SLOT_WIDTH, SLOT_HEIGHT), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(BADGE_SIZE)
                    .background(color, CircleShape)
                    .border(BADGE_BORDER, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = character.displayName.take(1),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColorOn(color),
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
        }
    }
}

/**
 * Text that reads on a character's colour. These sit on the character's own fixed colour rather than
 * a themed surface, so the choice follows that colour, not the theme: dark on the light ones (Tidus,
 * Yuna, Wakka, Rikku), white on the rest.
 */
private fun contentColorOn(background: Color): Color =
    if (background.luminance() > 0.4f) Color.Black.copy(alpha = 0.87f) else Color.White
