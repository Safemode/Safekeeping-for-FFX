package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.safemode.safekeepingforffx.data.reference.NodeType

/**
 * Sizing knobs for the sphere-grid nodes and the icons stamped on them. Change a number here and
 * rebuild - the whole grid redraws. These are world-space sizes (before zoom), so they scale with
 * pan/zoom along with everything else on the canvas.
 *
 * Node radii - how big each kind of node is drawn:
 * - [STAT_RADIUS]     the small stat spheres (HP, Strength, Agility, ...)
 * - [ABILITY_RADIUS]  the larger ability nodes (magic, skills, specials)
 * - [LOCK_RADIUS]     the lock gates
 *
 * Icon scale - how big the mark inside a node is, as a multiple of that node's radius (the icon's
 * 40x40 source box spans this many radii). Lower a value if icons crowd or overlap their neighbours;
 * raise it to fill the node more:
 * - [STAT_ICON_SCALE]     icon size on stat nodes
 * - [ABILITY_ICON_SCALE]  icon size on ability nodes - kept smaller than the others because that
 *                         artwork fills more of its box and the nodes are the largest, so at the
 *                         same scale it reaches into neighbouring nodes
 * - [LOCK_ICON_SCALE]     icon size on lock nodes
 */
object NodeSizing {
    const val STAT_RADIUS = 15f
    const val ABILITY_RADIUS = 24f
    const val LOCK_RADIUS = 13f

    const val STAT_ICON_SCALE = 1.8f
    const val ABILITY_ICON_SCALE = 1.35f
    const val LOCK_ICON_SCALE = 1.7f
}

/** The world-space radius this node kind is drawn at, from [NodeSizing]. */
fun NodeType.nodeRadius(): Float = when {
    isAbility -> NodeSizing.ABILITY_RADIUS
    isLock -> NodeSizing.LOCK_RADIUS
    else -> NodeSizing.STAT_RADIUS
}

/** The icon scale this node kind uses, from [NodeSizing]. */
fun NodeType.iconScale(): Float = when {
    isAbility -> NodeSizing.ABILITY_ICON_SCALE
    isLock -> NodeSizing.LOCK_ICON_SCALE
    else -> NodeSizing.STAT_ICON_SCALE
}

/** Black on light nodes, white on dark ones, so a node's icon always has contrast against its fill. */
fun glyphColorFor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color(0xFF1A1A1A) else Color.White
}

/**
 * Stamps a node's icon centred in the node, tinted to [color] for contrast against the fill and
 * scaled so the 40x40 source box spans [scale] * [radius] pixels - a little under the node's
 * diameter, so the mark sits inside the activation ring rather than touching it. Tune [scale] per
 * node kind in [NodeSizing].
 */
fun DrawScope.drawNodeIcon(
    painter: Painter,
    center: Offset,
    radius: Float,
    color: Color,
    scale: Float = NodeSizing.STAT_ICON_SCALE
) {
    val side = radius * scale
    translate(left = center.x - side / 2f, top = center.y - side / 2f) {
        with(painter) {
            draw(size = Size(side, side), colorFilter = ColorFilter.tint(color))
        }
    }
}

/** Which side of a node its label sits on - chosen to point into open space away from neighbours. */
enum class LabelPlacement { DOWN, UP, LEFT, RIGHT }

/**
 * One place to tune the node value/name labels while testing on a device: change a number, rebuild,
 * and the whole grid updates. Every value except [MIN_RADIUS] is a multiple of a node's on-screen
 * radius, so labels scale naturally with zoom.
 *
 * - [HEIGHT_FACTOR]      text height as a fraction of the node radius (smaller = smaller labels)
 * - [MAX_WIDTH_FACTOR]   widest a label may get before it shrinks to fit (stops long names sprawling)
 * - [GAP_FACTOR]         space between the node edge and its label
 * - [MIN_RADIUS]         hide labels once a node is smaller than this many pixels on screen; lower
 *                        this to show labels when more zoomed out, raise it to show them only when
 *                        zoomed further in
 *
 * The *side* a label lands on is decided in labelPlacementFor() in SphereGridScreen.kt - adjust the
 * heuristic there if you want to change how the direction is chosen.
 */
object LabelTuning {
    const val HEIGHT_FACTOR = 0.6f
    const val MAX_WIDTH_FACTOR = 3.4f
    const val GAP_FACTOR = 0.26f
    const val MIN_RADIUS = 13f
}

/**
 * Draws a small text label beside a node - the stat amount ("+2") or the ability name - on the side
 * given by [placement]. Scaled to the node's size and capped in width, so a long name shrinks rather
 * than sprawling over its neighbours.
 */
fun DrawScope.drawNodeLabel(
    result: TextLayoutResult,
    center: Offset,
    nodeRadius: Float,
    color: Color,
    placement: LabelPlacement
) {
    if (result.size.height == 0 || result.size.width == 0) return
    val targetHeight = nodeRadius * LabelTuning.HEIGHT_FACTOR
    val maxWidth = nodeRadius * LabelTuning.MAX_WIDTH_FACTOR
    val factor = minOf(targetHeight / result.size.height, maxWidth / result.size.width)
    val scaledW = result.size.width * factor
    val scaledH = result.size.height * factor
    val gap = nodeRadius * LabelTuning.GAP_FACTOR
    val labelCenter = when (placement) {
        LabelPlacement.DOWN -> Offset(center.x, center.y + nodeRadius + gap + scaledH / 2f)
        LabelPlacement.UP -> Offset(center.x, center.y - nodeRadius - gap - scaledH / 2f)
        LabelPlacement.RIGHT -> Offset(center.x + nodeRadius + gap + scaledW / 2f, center.y)
        LabelPlacement.LEFT -> Offset(center.x - nodeRadius - gap - scaledW / 2f, center.y)
    }
    scale(factor, pivot = labelCenter) {
        drawText(
            textLayoutResult = result,
            color = color,
            topLeft = Offset(
                x = labelCenter.x - result.size.width / 2f,
                y = labelCenter.y - result.size.height / 2f
            )
        )
    }
}
