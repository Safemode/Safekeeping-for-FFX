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

/** Black on light nodes, white on dark ones, so a node's icon always has contrast against its fill. */
fun glyphColorFor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color(0xFF1A1A1A) else Color.White
}

/**
 * Stamps a node's icon centred in the node, tinted to [color] for contrast against the fill and
 * scaled so the 40x40 source box spans [side] pixels - a little under the node's diameter, so the
 * mark sits inside the activation ring rather than touching it.
 */
fun DrawScope.drawNodeIcon(painter: Painter, center: Offset, radius: Float, color: Color) {
    val side = radius * 1.8f
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
