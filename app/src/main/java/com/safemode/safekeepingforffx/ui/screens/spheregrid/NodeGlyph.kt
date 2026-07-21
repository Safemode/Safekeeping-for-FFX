package com.safemode.safekeepingforffx.ui.screens.spheregrid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.safemode.safekeepingforffx.data.reference.NodeType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The mark drawn inside a node. HP and MP read best as their letters; every other type gets its own
 * little vector symbol, drawn from scratch here to evoke the familiar sphere-grid metaphors - a
 * sword for Strength, a shield for Defense, a circled triangle for Magic, a shielded triangle for
 * Magic Defense, a winged boot for Agility, a crosshair for Accuracy, wind for Evasion, a star for
 * Luck, and - for the four ability families - a light emblem on a dark disc: a sparkle-star for
 * White Magic, a fuller star for Black Magic, crossed swords for Skill, a ringed bar for Special.
 * These are original simplified glyphs, not the game's own artwork.
 *
 * Returns the letter for a lettered type, or null for a type that draws a [drawNodeSymbol] shape.
 */
fun NodeType.glyphLetter(): String? = when (this) {
    NodeType.HP -> "H"
    NodeType.MP -> "M"
    else -> null
}

/** Every letter and lock digit the grid can draw, so they can be measured once and cached. */
val GlyphLetters: List<String> = listOf("H", "M", "1", "2", "3", "4")

/** Black on light nodes, white on dark ones, so the glyph always has contrast. */
fun glyphColorFor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color(0xFF1A1A1A) else Color.White
}

/** Draws a pre-measured letter centred in a node and scaled to its radius. */
fun DrawScope.drawGlyphText(result: TextLayoutResult, center: Offset, radius: Float, color: Color) {
    val factor = (radius * 1.25f) / result.size.height
    scale(factor, pivot = center) {
        drawText(
            textLayoutResult = result,
            color = color,
            topLeft = Offset(
                x = center.x - result.size.width / 2f,
                y = center.y - result.size.height / 2f
            )
        )
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

/**
 * Draws the vector symbol for a symbol-type node. [background] is the node's fill colour, needed for
 * the couple of glyphs that carve into themselves (the Skill crescent, the leaf's vein). No-op for
 * lettered or empty types.
 */
fun DrawScope.drawNodeSymbol(
    type: NodeType,
    center: Offset,
    radius: Float,
    color: Color,
    background: Color
) {
    when (type) {
        NodeType.STRENGTH -> drawSword(center, radius, color)
        NodeType.DEFENSE -> drawShield(center, radius, color, filled = true)
        NodeType.MAGIC -> drawCircledTriangle(center, radius, color)
        NodeType.MAGIC_DEFENSE -> drawShieldedTriangle(center, radius, color)
        NodeType.AGILITY -> drawWingedFoot(center, radius, color)
        NodeType.ACCURACY -> drawCrosshair(center, radius, color)
        NodeType.EVASION -> drawWind(center, radius, color)
        NodeType.LUCK -> drawStar(center, radius, color)
        NodeType.WHITE_MAGIC -> drawWhiteMagic(center, radius)
        NodeType.BLACK_MAGIC -> drawBlackMagic(center, radius)
        NodeType.SKILL -> drawSkill(center, radius)
        NodeType.SPECIAL -> drawSpecial(center, radius)
        else -> Unit
    }
}

private fun DrawScope.drawSword(c: Offset, r: Float, color: Color) {
    val w = (r * 0.14f).coerceAtLeast(1.2f)
    val blade = Path().apply {
        moveTo(c.x, c.y - 0.78f * r)
        lineTo(c.x + 0.14f * r, c.y + 0.16f * r)
        lineTo(c.x - 0.14f * r, c.y + 0.16f * r)
        close()
    }
    drawPath(blade, color)
    drawLine(color, Offset(c.x - 0.42f * r, c.y + 0.2f * r), Offset(c.x + 0.42f * r, c.y + 0.2f * r), w, StrokeCap.Round)
    drawLine(color, Offset(c.x, c.y + 0.2f * r), Offset(c.x, c.y + 0.58f * r), w, StrokeCap.Round)
    drawCircle(color, radius = (0.1f * r).coerceAtLeast(1.2f), center = Offset(c.x, c.y + 0.62f * r))
}

private fun DrawScope.shieldPath(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x - 0.5f * r, c.y - 0.5f * r)
    lineTo(c.x + 0.5f * r, c.y - 0.5f * r)
    lineTo(c.x + 0.5f * r, c.y + 0.1f * r)
    lineTo(c.x, c.y + 0.66f * r)
    lineTo(c.x - 0.5f * r, c.y + 0.1f * r)
    close()
}

private fun DrawScope.drawShield(c: Offset, r: Float, color: Color, filled: Boolean) {
    val path = shieldPath(c, r)
    if (filled) {
        drawPath(path, color)
    } else {
        drawPath(path, color, style = Stroke(width = (r * 0.14f).coerceAtLeast(1.2f)))
    }
}

private fun DrawScope.trianglePath(c: Offset, r: Float, yShift: Float): Path = Path().apply {
    moveTo(c.x, c.y - 0.32f * r + yShift)
    lineTo(c.x + 0.32f * r, c.y + 0.26f * r + yShift)
    lineTo(c.x - 0.32f * r, c.y + 0.26f * r + yShift)
    close()
}

private fun DrawScope.drawCircledTriangle(c: Offset, r: Float, color: Color) {
    drawCircle(color, radius = 0.62f * r, center = c, style = Stroke(width = (r * 0.11f).coerceAtLeast(1.2f)))
    drawPath(trianglePath(c, r * 0.92f, yShift = 0.03f * r), color)
}

private fun DrawScope.drawShieldedTriangle(c: Offset, r: Float, color: Color) {
    drawShield(c, r, color, filled = false)
    drawPath(trianglePath(c, r * 0.6f, yShift = -0.02f * r), color)
}

private fun DrawScope.drawWingedFoot(c: Offset, r: Float, color: Color) {
    val w = (r * 0.11f).coerceAtLeast(1.1f)
    val boot = Path().apply {
        moveTo(c.x - 0.26f * r, c.y - 0.02f * r)
        lineTo(c.x - 0.06f * r, c.y - 0.02f * r)
        lineTo(c.x, c.y + 0.2f * r)
        lineTo(c.x + 0.5f * r, c.y + 0.24f * r)
        lineTo(c.x + 0.5f * r, c.y + 0.42f * r)
        lineTo(c.x - 0.42f * r, c.y + 0.42f * r)
        lineTo(c.x - 0.26f * r, c.y + 0.2f * r)
        close()
    }
    drawPath(boot, color)
    val ax = c.x - 0.18f * r
    val ay = c.y - 0.02f * r
    drawLine(color, Offset(ax, ay), Offset(c.x - 0.5f * r, c.y - 0.12f * r), w, StrokeCap.Round)
    drawLine(color, Offset(ax, ay), Offset(c.x - 0.42f * r, c.y - 0.34f * r), w, StrokeCap.Round)
    drawLine(color, Offset(ax, ay), Offset(c.x - 0.18f * r, c.y - 0.5f * r), w, StrokeCap.Round)
}

private fun DrawScope.drawCrosshair(c: Offset, r: Float, color: Color) {
    val w = (r * 0.1f).coerceAtLeast(1.1f)
    drawCircle(color, radius = 0.42f * r, center = c, style = Stroke(width = w))
    drawLine(color, Offset(c.x, c.y - 0.72f * r), Offset(c.x, c.y + 0.72f * r), w, StrokeCap.Round)
    drawLine(color, Offset(c.x - 0.72f * r, c.y), Offset(c.x + 0.72f * r, c.y), w, StrokeCap.Round)
}

private fun DrawScope.drawWind(c: Offset, r: Float, color: Color) {
    val w = (r * 0.13f).coerceAtLeast(1.2f)
    listOf(-0.25f, 0.05f, 0.35f).forEach { off ->
        val x = c.x + off * r
        val bow = Path().apply {
            moveTo(x, c.y - 0.45f * r)
            quadraticBezierTo(x + 0.22f * r, c.y, x, c.y + 0.45f * r)
        }
        drawPath(bow, color, style = Stroke(width = w, cap = StrokeCap.Round))
    }
}

// The four ability families share one look: a light emblem on a dark disc, so they read as a set.
private val EmblemDisc = Color(0xFF241E36)
private val EmblemStar = Color(0xFFF3EEFF)

private fun DrawScope.emblemDisc(c: Offset, r: Float) {
    drawCircle(EmblemDisc, radius = 0.72f * r, center = c)
}

/** A four-pointed star with points at N/E/S/W. [innerFactor] controls how full the points look. */
private fun DrawScope.fourPointStar(c: Offset, r: Float, outerFactor: Float, innerFactor: Float) {
    val outer = outerFactor * r
    val inner = innerFactor * r
    val path = Path()
    for (i in 0 until 8) {
        val angle = -PI / 2 + i * PI / 4
        val radius = if (i % 2 == 0) outer else inner
        val x = c.x + (radius * cos(angle)).toFloat()
        val y = c.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, EmblemStar)
}

/** White Magic: a thin sparkle-star on the dark disc. */
private fun DrawScope.drawWhiteMagic(c: Offset, r: Float) {
    emblemDisc(c, r)
    fourPointStar(c, r, outerFactor = 0.6f, innerFactor = 0.17f)
}

/** Black Magic: the same disc with a fuller, chunkier star, so it reads apart from White Magic. */
private fun DrawScope.drawBlackMagic(c: Offset, r: Float) {
    emblemDisc(c, r)
    fourPointStar(c, r, outerFactor = 0.62f, innerFactor = 0.28f)
}

/** Skill: two crossed swords on the dark disc. */
private fun DrawScope.drawSkill(c: Offset, r: Float) {
    emblemDisc(c, r)
    val w = (r * 0.12f).coerceAtLeast(1.5f)
    emblemSword(Offset(c.x + 0.5f * r, c.y - 0.5f * r), Offset(c.x - 0.5f * r, c.y + 0.52f * r), w)
    emblemSword(Offset(c.x - 0.5f * r, c.y - 0.5f * r), Offset(c.x + 0.5f * r, c.y + 0.52f * r), w)
}

private fun DrawScope.emblemSword(tip: Offset, hilt: Offset, w: Float) {
    drawLine(EmblemStar, tip, hilt, w, StrokeCap.Round)
    val dx = tip.x - hilt.x
    val dy = tip.y - hilt.y
    val len = kotlin.math.hypot(dx, dy)
    val px = -dy / len
    val py = dx / len
    // Crossguard a little above the hilt, and a pommel at the hilt end.
    val gx = hilt.x + 0.28f * dx
    val gy = hilt.y + 0.28f * dy
    val half = 0.16f * len
    drawLine(EmblemStar, Offset(gx - px * half, gy - py * half), Offset(gx + px * half, gy + py * half), w, StrokeCap.Round)
    drawCircle(EmblemStar, radius = w * 0.9f, center = hilt)
}

/** Special: a ringed bar - a circle crossed by a bar with two uprights - on the dark disc. */
private fun DrawScope.drawSpecial(c: Offset, r: Float) {
    emblemDisc(c, r)
    val w = (r * 0.11f).coerceAtLeast(1.2f)
    drawCircle(EmblemStar, radius = 0.46f * r, center = c, style = Stroke(width = w))
    drawLine(EmblemStar, Offset(c.x - 0.5f * r, c.y), Offset(c.x + 0.5f * r, c.y), w, StrokeCap.Round)
    drawLine(EmblemStar, Offset(c.x - 0.24f * r, c.y - 0.24f * r), Offset(c.x - 0.24f * r, c.y + 0.24f * r), w, StrokeCap.Round)
    drawLine(EmblemStar, Offset(c.x + 0.24f * r, c.y - 0.24f * r), Offset(c.x + 0.24f * r, c.y + 0.24f * r), w, StrokeCap.Round)
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val outer = 0.64f * r
    val inner = 0.27f * r
    val path = Path()
    for (i in 0 until 10) {
        val angle = -PI / 2 + i * PI / 5
        val radius = if (i % 2 == 0) outer else inner
        val x = c.x + (radius * cos(angle)).toFloat()
        val y = c.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
