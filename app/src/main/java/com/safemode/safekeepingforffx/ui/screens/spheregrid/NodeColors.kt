package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.compose.ui.graphics.Color
import com.safemode.safekeepingforffx.data.reference.GridCharacter
import com.safemode.safekeepingforffx.data.reference.NodeType

/**
 * Each character's in-game Sphere Grid colour, used to tint their activated nodes and the links
 * between them so several paths stay tellable apart. Values are lifted toward the light end of each
 * hue so they read on the dark grid backing - Kimahri's canonical dark blue in particular is brought
 * up to a mid blue, since a true navy vanishes against the near-black background.
 */
fun GridCharacter.activationColor(): Color = when (this) {
    GridCharacter.TIDUS -> Color(0xFF2FE1E1)   // aqua
    GridCharacter.YUNA -> Color(0xFFF2F2F2)    // white
    GridCharacter.AURON -> Color(0xFFFF5B52)   // red
    GridCharacter.KIMAHRI -> Color(0xFF5C8DEF) // dark blue (brightened for contrast)
    GridCharacter.WAKKA -> Color(0xFFFFDD3C)   // yellow
    GridCharacter.LULU -> Color(0xFFB57BFF)    // purple
    GridCharacter.RIKKU -> Color(0xFF57E389)   // green
}

/**
 * Placeholder colours for each node type, chosen to echo the legend on the reference image without
 * transcribing any of its text. The centres stay blank - a green sphere reads as "HP" the same way
 * it does on the grid itself, which is all a planner needs.
 */
fun NodeType.color(): Color = when (this) {
    NodeType.HP -> Color(0xFF66BB6A)
    NodeType.MP -> Color(0xFF29B6F6)
    NodeType.STRENGTH -> Color(0xFFEF5350)
    NodeType.DEFENSE -> Color(0xFF5C6BC0)
    NodeType.MAGIC -> Color(0xFFEC407A)
    NodeType.MAGIC_DEFENSE -> Color(0xFF3F51B5)
    NodeType.AGILITY -> Color(0xFFFDD835)
    NodeType.ACCURACY -> Color(0xFFFFB300)
    NodeType.EVASION -> Color(0xFFFFF176)
    NodeType.LUCK -> Color(0xFFFFD54F)
    NodeType.WHITE_MAGIC -> Color(0xFFCE93D8)
    NodeType.BLACK_MAGIC -> Color(0xFF5E35B1)
    NodeType.SKILL -> Color(0xFFF06292)
    NodeType.SPECIAL -> Color(0xFFFF8A65)
    NodeType.LOCK -> Color(0xFF373B4D)
    NodeType.EMPTY -> Color(0xFF78909C)
}

/** A short, human-readable label for the legend. Node centres themselves stay unlabelled. */
fun NodeType.legendLabel(): String = when (this) {
    NodeType.HP -> "HP"
    NodeType.MP -> "MP"
    NodeType.STRENGTH -> "Strength"
    NodeType.DEFENSE -> "Defense"
    NodeType.MAGIC -> "Magic"
    NodeType.MAGIC_DEFENSE -> "Magic Def"
    NodeType.AGILITY -> "Agility"
    NodeType.ACCURACY -> "Accuracy"
    NodeType.EVASION -> "Evasion"
    NodeType.LUCK -> "Luck"
    NodeType.WHITE_MAGIC -> "White Magic"
    NodeType.BLACK_MAGIC -> "Black Magic"
    NodeType.SKILL -> "Skill"
    NodeType.SPECIAL -> "Special"
    NodeType.LOCK -> "Lock"
    NodeType.EMPTY -> "Empty"
}
