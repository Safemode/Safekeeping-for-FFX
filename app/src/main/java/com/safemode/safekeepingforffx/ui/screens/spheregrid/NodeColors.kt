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
 * The fill colour of each node type - the sphere's colour on the grid and its swatch in the legend.
 * Change a value here and rebuild; every node of that type updates. Values are 0xAARRGGBB, so keep
 * the leading FF for a fully opaque node.
 *
 * These are placeholder colours chosen to echo the legend on the reference image. Node centres stay
 * blank - a green sphere reads as "HP" the same way it does on the grid itself. The icon drawn on
 * top auto-contrasts against the fill (dark on light, light on dark) unless the type is forced white
 * in AlwaysWhiteIcons - see NodeGlyph.kt.
 */
object NodeColors {
    val HP = Color(0xFF66BB6A)
    val MP = Color(0xFF66BB6A)
    val STRENGTH = Color(0xFFEF5350)
    val DEFENSE = Color(0xFF5C6BC0)
    val MAGIC = Color(0xFFEF5350)
    val MAGIC_DEFENSE = Color(0xFF5C6BC0)
    val AGILITY = Color(0xFFFDD835)
    val ACCURACY = Color(0xFFFDD835)
    val EVASION = Color(0xFFFDD835)
    val LUCK = Color(0xFFFDD835)
    val WHITE_MAGIC = Color(0xFFCE93D8)
    val BLACK_MAGIC = Color(0xFF5E35B1)
    val SKILL = Color(0xFFF06292)
    val SPECIAL = Color(0xFFF06292)
    val LOCK = Color(0xFF373B4D)
    val EMPTY = Color(0xFF78909C)
}

/** The fill colour for a node type, from the [NodeColors] config. */
fun NodeType.color(): Color = when (this) {
    NodeType.HP -> NodeColors.HP
    NodeType.MP -> NodeColors.MP
    NodeType.STRENGTH -> NodeColors.STRENGTH
    NodeType.DEFENSE -> NodeColors.DEFENSE
    NodeType.MAGIC -> NodeColors.MAGIC
    NodeType.MAGIC_DEFENSE -> NodeColors.MAGIC_DEFENSE
    NodeType.AGILITY -> NodeColors.AGILITY
    NodeType.ACCURACY -> NodeColors.ACCURACY
    NodeType.EVASION -> NodeColors.EVASION
    NodeType.LUCK -> NodeColors.LUCK
    NodeType.WHITE_MAGIC -> NodeColors.WHITE_MAGIC
    NodeType.BLACK_MAGIC -> NodeColors.BLACK_MAGIC
    NodeType.SKILL -> NodeColors.SKILL
    NodeType.SPECIAL -> NodeColors.SPECIAL
    NodeType.LOCK -> NodeColors.LOCK
    NodeType.EMPTY -> NodeColors.EMPTY
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
