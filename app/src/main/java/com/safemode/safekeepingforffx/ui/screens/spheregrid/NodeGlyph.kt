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
 * 40x40 source box spans this many radii). Every icon has its own value, so art that fills its box
 * differently can be dialled in one at a time. Lower a value if that icon crowds or overlaps its
 * neighbours; raise it to fill the node more. The constants are grouped stat / ability / lock only
 * for readability - each is independent.
 */
object NodeSizing {
    const val STAT_RADIUS = 13f
    const val ABILITY_RADIUS = 18f
    const val LOCK_RADIUS = 12f

    // Fallback for the Empty node (draws no icon) and drawNodeIcon's default parameter.
    const val DEFAULT_ICON_SCALE = 1.8f

    // Stat icon scales
    const val HP_ICON_SCALE = 1.8f
    const val MP_ICON_SCALE = 1.8f
    const val STRENGTH_ICON_SCALE = 2.2f
    const val DEFENSE_ICON_SCALE = 2f
    const val MAGIC_ICON_SCALE = 2.2f
    const val MAGIC_DEFENSE_ICON_SCALE = 2f
    const val AGILITY_ICON_SCALE = 2.2f
    const val ACCURACY_ICON_SCALE = 2.2f
    const val EVASION_ICON_SCALE = 2.2f
    const val LUCK_ICON_SCALE = 2.2f

    // Ability icon scales
    const val WHITE_MAGIC_ICON_SCALE = 2.25f
    const val BLACK_MAGIC_ICON_SCALE = 2.25f
    const val SKILL_ICON_SCALE = 2.25f
    const val SPECIAL_ICON_SCALE = 2.25f

    // Lock icon scale
    const val LOCK_ICON_SCALE = 1.7f

    // Legend swatches are all the same small circle, so every icon there uses this one scale rather
    // than its per-node scale (some of which are large enough to overflow the tiny swatch). Keep it
    // low enough that even the fullest icon fits inside the swatch circle.
    const val LEGEND_ICON_SCALE = 1.5f

    // Outline thickness per node type, as a fraction of the icon's on-screen size. Every icon gets a
    // black outline (see NodeType.iconOutline); raise a value for a heavier outline on that icon,
    // lower it for a thinner one. Grouped stat / ability / lock only for readability.
    const val DEFAULT_ICON_OUTLINE_FACTOR = 0.045f

    // Stat icon outlines
    const val HP_ICON_OUTLINE_FACTOR = 0.045f
    const val MP_ICON_OUTLINE_FACTOR = 0.045f
    const val STRENGTH_ICON_OUTLINE_FACTOR = 0.045f
    const val DEFENSE_ICON_OUTLINE_FACTOR = 0.045f
    const val MAGIC_ICON_OUTLINE_FACTOR = 0.045f
    const val MAGIC_DEFENSE_ICON_OUTLINE_FACTOR = 0.045f
    const val AGILITY_ICON_OUTLINE_FACTOR = 0.045f
    const val ACCURACY_ICON_OUTLINE_FACTOR = 0.040f
    const val EVASION_ICON_OUTLINE_FACTOR = 0.045f
    const val LUCK_ICON_OUTLINE_FACTOR = 0.045f

    // Ability icon outlines
    const val WHITE_MAGIC_ICON_OUTLINE_FACTOR = 0.045f
    const val BLACK_MAGIC_ICON_OUTLINE_FACTOR = 0.035f
    const val SKILL_ICON_OUTLINE_FACTOR = 0.035f
    const val SPECIAL_ICON_OUTLINE_FACTOR = 0.035f

    // Lock icon outline
    const val LOCK_ICON_OUTLINE_FACTOR = 0.045f
}

/** The world-space radius this node kind is drawn at, from [NodeSizing]. */
fun NodeType.nodeRadius(): Float = when {
    isAbility -> NodeSizing.ABILITY_RADIUS
    isLock -> NodeSizing.LOCK_RADIUS
    else -> NodeSizing.STAT_RADIUS
}

/** The icon scale this node type uses, from [NodeSizing]. */
fun NodeType.iconScale(): Float = when (this) {
    NodeType.HP -> NodeSizing.HP_ICON_SCALE
    NodeType.MP -> NodeSizing.MP_ICON_SCALE
    NodeType.STRENGTH -> NodeSizing.STRENGTH_ICON_SCALE
    NodeType.DEFENSE -> NodeSizing.DEFENSE_ICON_SCALE
    NodeType.MAGIC -> NodeSizing.MAGIC_ICON_SCALE
    NodeType.MAGIC_DEFENSE -> NodeSizing.MAGIC_DEFENSE_ICON_SCALE
    NodeType.AGILITY -> NodeSizing.AGILITY_ICON_SCALE
    NodeType.ACCURACY -> NodeSizing.ACCURACY_ICON_SCALE
    NodeType.EVASION -> NodeSizing.EVASION_ICON_SCALE
    NodeType.LUCK -> NodeSizing.LUCK_ICON_SCALE
    NodeType.WHITE_MAGIC -> NodeSizing.WHITE_MAGIC_ICON_SCALE
    NodeType.BLACK_MAGIC -> NodeSizing.BLACK_MAGIC_ICON_SCALE
    NodeType.SKILL -> NodeSizing.SKILL_ICON_SCALE
    NodeType.SPECIAL -> NodeSizing.SPECIAL_ICON_SCALE
    NodeType.LOCK -> NodeSizing.LOCK_ICON_SCALE
    NodeType.EMPTY -> NodeSizing.DEFAULT_ICON_SCALE
}

/** Black on light nodes, white on dark ones, so a node's icon always has contrast against its fill. */
fun glyphColorFor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color(0xFF1A1A1A) else Color.White
}

/**
 * Node types whose icon is always drawn white, overriding the automatic light/dark contrast choice
 * in [glyphColorFor] - used where a white mark is preferred over the contrast default.
 */
private val AlwaysWhiteIcons = setOf(
    NodeType.WHITE_MAGIC,
    NodeType.BLACK_MAGIC,
    NodeType.SKILL,
    NodeType.SPECIAL,
    NodeType.ACCURACY,
    NodeType.AGILITY,
    NodeType.EVASION,
    NodeType.LUCK,
)

/**
 * The colour a node's icon is tinted: forced white for the types in [AlwaysWhiteIcons], otherwise
 * chosen to contrast with the node's [background] via [glyphColorFor].
 */
fun NodeType.iconColor(background: Color): Color =
    if (this in AlwaysWhiteIcons) Color.White else glyphColorFor(background)

/**
 * The outline colour for a node type's icon. Every icon gets a black outline for contrast against
 * its node fill; the outline thickness is set per type by [iconOutlineFactor].
 */
fun NodeType.iconOutline(): Color = Color.Black

/** The outline thickness this node type uses, from [NodeSizing]. */
fun NodeType.iconOutlineFactor(): Float = when (this) {
    NodeType.HP -> NodeSizing.HP_ICON_OUTLINE_FACTOR
    NodeType.MP -> NodeSizing.MP_ICON_OUTLINE_FACTOR
    NodeType.STRENGTH -> NodeSizing.STRENGTH_ICON_OUTLINE_FACTOR
    NodeType.DEFENSE -> NodeSizing.DEFENSE_ICON_OUTLINE_FACTOR
    NodeType.MAGIC -> NodeSizing.MAGIC_ICON_OUTLINE_FACTOR
    NodeType.MAGIC_DEFENSE -> NodeSizing.MAGIC_DEFENSE_ICON_OUTLINE_FACTOR
    NodeType.AGILITY -> NodeSizing.AGILITY_ICON_OUTLINE_FACTOR
    NodeType.ACCURACY -> NodeSizing.ACCURACY_ICON_OUTLINE_FACTOR
    NodeType.EVASION -> NodeSizing.EVASION_ICON_OUTLINE_FACTOR
    NodeType.LUCK -> NodeSizing.LUCK_ICON_OUTLINE_FACTOR
    NodeType.WHITE_MAGIC -> NodeSizing.WHITE_MAGIC_ICON_OUTLINE_FACTOR
    NodeType.BLACK_MAGIC -> NodeSizing.BLACK_MAGIC_ICON_OUTLINE_FACTOR
    NodeType.SKILL -> NodeSizing.SKILL_ICON_OUTLINE_FACTOR
    NodeType.SPECIAL -> NodeSizing.SPECIAL_ICON_OUTLINE_FACTOR
    NodeType.LOCK -> NodeSizing.LOCK_ICON_OUTLINE_FACTOR
    NodeType.EMPTY -> NodeSizing.DEFAULT_ICON_OUTLINE_FACTOR
}

/**
 * Stamps a node's icon centred in the node, tinted to [color] for contrast against the fill and
 * scaled so the 40x40 source box spans [scale] * [radius] pixels - a little under the node's
 * diameter, so the mark sits inside the activation ring rather than touching it. Tune [scale] per
 * node kind in [NodeSizing].
 *
 * If [outline] is set, the icon is first stamped in that colour at eight small offsets around the
 * centre, so the tinted fill on top reads with an even outline - used to keep white icons legible on
 * light node fills. Outline thickness is [outlineFactor] of the icon size.
 */
fun DrawScope.drawNodeIcon(
    painter: Painter,
    center: Offset,
    radius: Float,
    color: Color,
    scale: Float = NodeSizing.DEFAULT_ICON_SCALE,
    outline: Color? = null,
    outlineFactor: Float = NodeSizing.DEFAULT_ICON_OUTLINE_FACTOR
) {
    val side = radius * scale
    val left = center.x - side / 2f
    val top = center.y - side / 2f
    fun stamp(dx: Float, dy: Float, tint: Color) {
        translate(left = left + dx, top = top + dy) {
            with(painter) { draw(size = Size(side, side), colorFilter = ColorFilter.tint(tint)) }
        }
    }
    if (outline != null) {
        val d = (side * outlineFactor).coerceAtLeast(0.6f)
        stamp(-d, 0f, outline); stamp(d, 0f, outline); stamp(0f, -d, outline); stamp(0f, d, outline)
        stamp(-d, -d, outline); stamp(d, -d, outline); stamp(-d, d, outline); stamp(d, d, outline)
    }
    stamp(0f, 0f, color)
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
 * than sprawling over its neighbours. Multi-line labels (a wrapped ability name) are sized so each
 * line is about [LabelTuning.HEIGHT_FACTOR] of the node radius, rather than shrinking the whole block
 * to one line's worth of height.
 */
fun DrawScope.drawNodeLabel(
    result: TextLayoutResult,
    center: Offset,
    nodeRadius: Float,
    color: Color,
    placement: LabelPlacement
) {
    if (result.size.height == 0 || result.size.width == 0) return
    val lineCount = result.lineCount.coerceAtLeast(1)
    val targetHeight = nodeRadius * LabelTuning.HEIGHT_FACTOR * lineCount
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
