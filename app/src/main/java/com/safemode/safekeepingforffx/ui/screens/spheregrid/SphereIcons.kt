package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.safemode.safekeepingforffx.R
import com.safemode.safekeepingforffx.data.reference.NodeType

/**
 * The sphere-grid node icons, loaded once as vector drawables so the canvas can stamp them into each
 * node and the legend. The artwork is the FFX sphere-grid icon set from Grayfox96's Sphere Grid
 * viewer (github.com/Grayfox96/FFX-Sphere-Grid-viewer), converted to Android vector drawables.
 *
 * [forType] returns the icon for a stat or ability type, or null for a type that draws no mark
 * (Empty). [forLock] is indexed by lock level (1-4).
 */
class SphereIcons(
    private val byType: Map<NodeType, Painter>,
    private val locks: List<Painter>
) {
    fun forType(type: NodeType): Painter? = byType[type]
    fun forLock(level: Int): Painter? = locks.getOrNull(level - 1)
}

/** Loads every node icon from resources once and remembers them for the sphere-grid canvas. */
@Composable
fun rememberSphereIcons(): SphereIcons = SphereIcons(
    byType = mapOf(
        NodeType.HP to painterResource(R.drawable.ic_sg_hp),
        NodeType.MP to painterResource(R.drawable.ic_sg_mp),
        NodeType.STRENGTH to painterResource(R.drawable.ic_sg_strength),
        NodeType.DEFENSE to painterResource(R.drawable.ic_sg_defense),
        NodeType.MAGIC to painterResource(R.drawable.ic_sg_magic),
        NodeType.MAGIC_DEFENSE to painterResource(R.drawable.ic_sg_magic_defense),
        NodeType.AGILITY to painterResource(R.drawable.ic_sg_agility),
        NodeType.ACCURACY to painterResource(R.drawable.ic_sg_accuracy),
        NodeType.EVASION to painterResource(R.drawable.ic_sg_evasion),
        NodeType.LUCK to painterResource(R.drawable.ic_sg_luck),
        NodeType.WHITE_MAGIC to painterResource(R.drawable.ic_sg_white_magic),
        NodeType.BLACK_MAGIC to painterResource(R.drawable.ic_sg_black_magic),
        NodeType.SKILL to painterResource(R.drawable.ic_sg_skill),
        NodeType.SPECIAL to painterResource(R.drawable.ic_sg_special),
    ),
    locks = listOf(
        painterResource(R.drawable.ic_sg_lock_1),
        painterResource(R.drawable.ic_sg_lock_2),
        painterResource(R.drawable.ic_sg_lock_3),
        painterResource(R.drawable.ic_sg_lock_4),
    )
)
