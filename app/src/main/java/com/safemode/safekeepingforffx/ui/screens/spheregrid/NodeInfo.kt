package com.safemode.safekeepingforffx.ui.screens.spheregrid

import com.safemode.safekeepingforffx.data.reference.NodeContent
import com.safemode.safekeepingforffx.data.reference.NodeType

/**
 * The attribute values the editor offers, matching what the game's spheres write: HP in 200/300,
 * MP in 10/20/40 (the Expert grid adds MP+10 nodes), and every other stat in +1..+4.
 */
val AttributeCatalog: List<NodeContent.Attribute> = buildList {
    add(NodeContent.Attribute(NodeType.HP, 200))
    add(NodeContent.Attribute(NodeType.HP, 300))
    add(NodeContent.Attribute(NodeType.MP, 10))
    add(NodeContent.Attribute(NodeType.MP, 20))
    add(NodeContent.Attribute(NodeType.MP, 40))
    listOf(
        NodeType.STRENGTH, NodeType.DEFENSE, NodeType.MAGIC, NodeType.MAGIC_DEFENSE,
        NodeType.AGILITY, NodeType.ACCURACY, NodeType.EVASION, NodeType.LUCK
    ).forEach { attr ->
        for (value in 1..4) add(NodeContent.Attribute(attr, value))
    }
}

/** Full attribute name for a stat node, e.g. "Magic Defense" (the legend uses a shorter form). */
fun NodeType.attributeName(): String = when (this) {
    NodeType.HP -> "HP"
    NodeType.MP -> "MP"
    NodeType.STRENGTH -> "Strength"
    NodeType.DEFENSE -> "Defense"
    NodeType.MAGIC -> "Magic"
    NodeType.MAGIC_DEFENSE -> "Magic Defense"
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

/** The content written on the node, e.g. "Strength +2", "Fira", "Empty", "Lv.2 Lock". */
fun NodeContent.label(): String = when (this) {
    NodeContent.Empty -> "Empty"
    is NodeContent.Attribute -> "${attribute.attributeName()} +$value"
    is NodeContent.Ability -> name
    is NodeContent.Lock -> "Lv.$level Lock"
}

/** A short description of what kind of node this is, for the detail header. */
fun NodeContent.kindLabel(): String = when (this) {
    NodeContent.Empty -> "Empty node"
    is NodeContent.Attribute -> "${attribute.attributeName()} node"
    is NodeContent.Ability -> "${family.attributeName()} ability"
    is NodeContent.Lock -> "Level $level lock"
}

/**
 * The spheres that activate this node, per the Standard Sphere Grid rules:
 * every stat also takes an Attribute Sphere, and every ability also takes an Ability Sphere.
 */
fun NodeContent.activationSpheres(): List<String> = when (this) {
    NodeContent.Empty -> emptyList()
    is NodeContent.Lock -> listOf("Lv.$level Key Sphere")
    is NodeContent.Attribute -> when (attribute) {
        NodeType.HP, NodeType.STRENGTH, NodeType.DEFENSE ->
            listOf("Power Sphere", "Attribute Sphere")
        NodeType.MP, NodeType.MAGIC, NodeType.MAGIC_DEFENSE ->
            listOf("Mana Sphere", "Attribute Sphere")
        NodeType.AGILITY, NodeType.EVASION, NodeType.ACCURACY ->
            listOf("Speed Sphere", "Attribute Sphere")
        NodeType.LUCK -> listOf("Fortune Sphere", "Attribute Sphere")
        else -> listOf("Attribute Sphere")
    }
    is NodeContent.Ability -> when (family) {
        NodeType.WHITE_MAGIC -> listOf("Ability Sphere", "White Magic Sphere")
        NodeType.BLACK_MAGIC -> listOf("Ability Sphere", "Black Magic Sphere")
        NodeType.SKILL -> listOf("Ability Sphere", "Skill Sphere")
        NodeType.SPECIAL -> listOf("Ability Sphere", "Special Sphere")
        else -> listOf("Ability Sphere")
    }
}

/** One line explaining how to activate the node, or that there's nothing to activate. */
fun NodeContent.activationSentence(): String {
    val spheres = activationSpheres()
    return when {
        this is NodeContent.Empty ->
            "An empty node - nothing to activate. You can write new content onto it."
        this is NodeContent.Lock ->
            "Opened in-game with a ${spheres.first()}. Unlocking here turns it into a blank node - " +
                "shared by every character - that you can then edit."
        spheres.size >= 2 ->
            "Activate with a ${spheres[0]} or a ${spheres[1]}."
        spheres.size == 1 ->
            "Activate with a ${spheres[0]}."
        else -> ""
    }
}
