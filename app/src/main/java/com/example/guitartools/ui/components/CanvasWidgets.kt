package com.example.guitartools.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitartools.model.MusicTheory
import com.example.guitartools.model.Voicing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// Theme Colors matching Python app
val AppBg = Color(0xFF0F1218)
val AppSurface = Color(0xFF21242E)
val AppSurface2 = Color(0xFF303642)
val AppAccent = Color(0xFF4C9EFF)
val AppGood = Color(0xFF3DCF85)
val AppWarn = Color(0xFFFFB840)
val AppBad = Color(0xFFFF6B5C)
val AppText = Color(0xFFEDEDF5)
val AppMuted = Color(0xFF8C94A3)
val AppDark = Color(0xFF0D0F14)

private fun Color.toArgbInt(): Int {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

@Composable
fun MeterGauge(
    cents: Float,
    active: Boolean,
    inTune: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height - 16.dp.toPx()
        val r = minOf(size.width * 0.44f, size.height * 0.86f)
        val clampedCents = cents.coerceIn(-50f, 50f)

        val zones = listOf(
            Triple(-50f, -25f, AppBad),
            Triple(-25f, -5f, AppWarn),
            Triple(-5f, 5f, AppGood),
            Triple(5f, 25f, AppWarn),
            Triple(25f, 50f, AppBad)
        )

        // Arc Zones
        for ((a, b, col) in zones) {
            val lit = active && clampedCents in a..b
            val alpha = if (lit) 1.0f else 0.40f
            val steps = maxOf(2, ((b - a) / 2f).toInt())
            val path = Path()

            for (s in 0..steps) {
                val deg = (a + (b - a) * s / steps) * 0.9f
                val rad = (deg - 90f) * PI.toFloat() / 180f
                val px = cx + r * cos(rad)
                val py = cy + r * sin(rad)
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }

            drawPath(
                path = path,
                color = col.copy(alpha = alpha),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Ticks
        for (t in -50..50 step 10) {
            val isLong = t in listOf(-50, 0, 50)
            val deg = t * 0.9f
            val rad = (deg - 90f) * PI.toFloat() / 180f
            val innerR = r - (if (isLong) 24.dp.toPx() else 17.dp.toPx())
            val outerR = r - 12.dp.toPx()

            val p1 = Offset(cx + innerR * cos(rad), cy + innerR * sin(rad))
            val p2 = Offset(cx + outerR * cos(rad), cy + outerR * sin(rad))

            drawLine(
                color = AppMuted,
                start = p1,
                end = p2,
                strokeWidth = if (isLong) 2.dp.toPx() else 1.dp.toPx()
            )
        }

        // Labels 'b' and '#'
        val paint = Paint().apply {
            color = AppMuted.toArgbInt()
            textSize = 14.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val bRad = (-55f * 0.9f - 90f) * PI.toFloat() / 180f
        val bR = r + 2.dp.toPx()
        drawContext.canvas.nativeCanvas.drawText("b", cx + bR * cos(bRad), cy + bR * sin(bRad), paint)

        val sRad = (55f * 0.9f - 90f) * PI.toFloat() / 180f
        drawContext.canvas.nativeCanvas.drawText("#", cx + bR * cos(sRad), cy + bR * sin(sRad), paint)

        // Needle
        if (active) {
            val col = if (inTune) AppGood else AppText
            val deg = clampedCents * 0.9f
            val rad = (deg - 90f) * PI.toFloat() / 180f
            val tipR = r - 14.dp.toPx()
            val tip = Offset(cx + tipR * cos(rad), cy + tipR * sin(rad))

            // Needle Glow
            drawLine(
                color = col.copy(alpha = 0.25f),
                start = Offset(cx, cy),
                end = tip,
                strokeWidth = 7.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Needle Core
            drawLine(
                color = col,
                start = Offset(cx, cy),
                end = tip,
                strokeWidth = 2.6f.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center Pin
        drawCircle(
            color = AppText,
            radius = 8.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}

@Composable
fun ChordDiagram(
    voicing: Voicing?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (voicing == null) return@Canvas
        val frets = voicing.frets
        val base = voicing.base
        val barre = voicing.barre
        val roots = voicing.roots
        val n = frets.size

        val ml = 18.dp.toPx()
        val mr = 8.dp.toPx()
        val mt = 24.dp.toPx()
        val mb = 6.dp.toPx()

        val gx = ml
        val gw = size.width - ml - mr
        val top = size.height - mt
        val gh = size.height - mt - mb

        val sx = gw / (n - 1)
        val fy = gh / 4f
        val r = minOf(sx, fy) * 0.36f

        // Nut and Frets (Horizontal lines)
        for (k in 0..4) {
            val isNut = (k == 0 && base == 1)
            val lineY = top - k * fy
            drawLine(
                color = if (isNut) AppText else AppMuted,
                start = Offset(gx, lineY),
                end = Offset(gx + gw, lineY),
                strokeWidth = if (isNut) 3.5f.dp.toPx() else 1.dp.toPx()
            )
        }

        // Strings (Vertical lines)
        for (i in 0 until n) {
            val lineX = gx + i * sx
            drawLine(
                color = AppMuted,
                start = Offset(lineX, top),
                end = Offset(lineX, top - 4 * fy),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Base fret indicator text
        if (base > 1) {
            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 13.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                base.toString(),
                ml * 0.45f,
                top - fy * 0.35f,
                paint
            )
        }

        // Barre
        if (barre != null) {
            val f = barre.fret
            val i0 = barre.stringStart
            val i1 = barre.stringEnd
            val y = top - (f - base + 0.5f) * fy
            val bx = gx + i0 * sx - r
            val bw = (i1 - i0) * sx + 2 * r
            val bh = 2 * r

            drawRoundRect(
                color = AppAccent,
                topLeft = Offset(bx, y - r),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(r, r)
            )
        }

        // Strings status & dots
        for (i in 0 until n) {
            val x = gx + i * sx
            val f = frets[i]

            if (f == null) {
                // Muted String 'X'
                val d = 5.dp.toPx()
                val yy = top + 12.dp.toPx()
                drawLine(
                    color = AppMuted,
                    start = Offset(x - d, yy - d),
                    end = Offset(x + d, yy + d),
                    strokeWidth = 1.5f.dp.toPx()
                )
                drawLine(
                    color = AppMuted,
                    start = Offset(x - d, yy + d),
                    end = Offset(x + d, yy - d),
                    strokeWidth = 1.5f.dp.toPx()
                )
            } else if (f == 0) {
                // Open String 'O'
                val col = if (roots[i]) AppWarn else AppText
                drawCircle(
                    color = col,
                    radius = 5.dp.toPx(),
                    center = Offset(x, top + 12.dp.toPx()),
                    style = Stroke(width = 1.5f.dp.toPx())
                )
            } else {
                // Fretted note dot
                val y = top - (f - base + 0.5f) * fy
                val col = if (roots[i]) AppWarn else AppAccent
                drawCircle(
                    color = col,
                    radius = r,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
fun Fretboard(
    strings: List<Pair<String, Int>>,
    pcs: Set<Int>,
    rootPc: Int,
    specialPcs: Set<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val n = strings.size
        if (n < 2) return@Canvas

        val ml = 30.dp.toPx()
        val mr = 8.dp.toPx()
        val mt = 10.dp.toPx()
        val mb = 22.dp.toPx()

        val gx = ml
        val gw = size.width - ml - mr
        val fw = gw / 12f
        val ytop = size.height - mt
        val ybot = mb
        val sy = (ytop - ybot) / (n - 1)
        val r = minOf(sy * 0.42f, fw * 0.44f)
        val ymid = (ytop + ybot) / 2f

        // Fret Markers (3, 5, 7, 9, 12)
        for (k in listOf(3, 5, 7, 9)) {
            val x = gx + (k - 0.5f) * fw
            drawCircle(color = AppSurface2, radius = 5.dp.toPx(), center = Offset(x, ymid))
        }
        // Double marker at 12th fret
        val x12 = gx + 11.5f * fw
        drawCircle(color = AppSurface2, radius = 5.dp.toPx(), center = Offset(x12, ymid - sy * 0.9f))
        drawCircle(color = AppSurface2, radius = 5.dp.toPx(), center = Offset(x12, ymid + sy * 0.9f))

        // Frets vertical lines (0-12)
        for (k in 0..12) {
            val isNut = (k == 0)
            val lineX = gx + k * fw
            drawLine(
                color = if (isNut) AppText else AppMuted,
                start = Offset(lineX, ybot),
                end = Offset(lineX, ytop),
                strokeWidth = if (isNut) 3.dp.toPx() else 1.dp.toPx()
            )
        }

        // Strings horizontal lines
        for (i in 0 until n) {
            val y = ybot + i * sy
            val thickness = (0.8f + 1.2f * (1f - i.toFloat() / (n - 1))).dp.toPx()
            drawLine(
                color = AppMuted,
                start = Offset(gx, y),
                end = Offset(gx + gw, y),
                strokeWidth = thickness
            )
        }

        // Fret numbers
        val fretTextPaint = Paint().apply {
            color = AppMuted.toArgbInt()
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        for (k in listOf(3, 5, 7, 9, 12)) {
            val x = gx + (k - 0.5f) * fw
            drawContext.canvas.nativeCanvas.drawText(k.toString(), x, mb * 0.4f, fretTextPaint)
        }

        // Notes on Fretboard
        val noteTextPaint = Paint().apply {
            color = AppDark.toArgbInt()
            textSize = 9.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        for ((i, stringPair) in strings.withIndex()) {
            val semis = stringPair.second
            val y = ybot + i * sy

            for (k in 0..12) {
                val pc = Math.floorMod(69 + semis + k, 12)
                if (pc !in pcs) continue

                val x = if (k == 0) gx - 15.dp.toPx() else gx + (k - 0.5f) * fw
                val col = when {
                    pc == rootPc -> AppWarn
                    pc in specialPcs -> AppGood
                    else -> AppAccent
                }

                drawCircle(
                    color = col,
                    radius = r,
                    center = Offset(x, y)
                )

                val noteName = MusicTheory.NOTE_NAMES[pc]
                val textY = y + (noteTextPaint.textSize / 3f)
                drawContext.canvas.nativeCanvas.drawText(noteName, x, textY, noteTextPaint)
            }
        }
    }
}

@Composable
fun CircleOfFifths(
    selectedIndex: Int,
    selectedMinor: Boolean,
    onSelectKey: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val bigR = minOf(size.width, size.height) * 0.48f

                    var bestDist = Float.MAX_VALUE
                    var bestIdx = -1
                    var bestIsMinor = false

                    val circles = mutableListOf<Triple<Int, Boolean, Offset>>()
                    for (minor in listOf(false, true)) {
                        val rad = if (minor) 0.50f else 0.80f
                        for (i in 0 until 12) {
                            val a = (i * 30) * PI.toFloat() / 180f
                            val x = cx + bigR * rad * sin(a)
                            val y = cy - bigR * rad * cos(a)
                            circles.add(Triple(i, minor, Offset(x, y)))
                        }
                    }

                    for ((i, minor, pos) in circles) {
                        val d = hypot(tapOffset.x - pos.x, tapOffset.y - pos.y)
                        val maxRadius = bigR * (if (minor) 0.125f else 0.19f) * 1.3f
                        if (d <= maxRadius && d < bestDist) {
                            bestDist = d
                            bestIdx = i
                            bestIsMinor = minor
                        }
                    }

                    if (bestIdx != -1) {
                        onSelectKey(bestIdx, bestIsMinor)
                    }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val bigR = minOf(size.width, size.height) * 0.48f

        // Outer Dark Circle
        drawCircle(color = Color(0xFF171A21), radius = bigR, center = Offset(cx, cy))

        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Draw Circles for Major (Outer) and Minor (Inner)
        for (minor in listOf(false, true)) {
            val rad = if (minor) 0.50f else 0.80f
            val rr = if (minor) 0.125f else 0.19f
            val rVal = bigR * rr

            for (i in 0 until 12) {
                val a = (i * 30) * PI.toFloat() / 180f
                val x = cx + bigR * rad * sin(a)
                val y = cy - bigR * rad * cos(a)

                val col = when {
                    i == selectedIndex && minor == selectedMinor -> AppAccent
                    i == selectedIndex -> AppAccent.copy(alpha = 0.40f)
                    minor == selectedMinor && (i - selectedIndex + 12) % 12 in listOf(1, 11) -> AppSurface2
                    else -> AppSurface
                }

                drawCircle(color = col, radius = rVal, center = Offset(x, y))

                val label = if (minor) MusicTheory.CIRCLE_MINOR[i] else MusicTheory.CIRCLE_MAJOR[i]
                textPaint.textSize = (if (minor) 11.dp.toPx() else 16.dp.toPx())
                textPaint.isFakeBoldText = !minor

                val textY = y + (textPaint.textSize / 3f)
                drawContext.canvas.nativeCanvas.drawText(label, x, textY, textPaint)
            }
        }

        // Center Signature Label (e.g. "0", "3#", "2b")
        val sigText = if (selectedIndex == 0) "0" else if (selectedIndex <= 6) "${selectedIndex}#" else "${12 - selectedIndex}b"
        val centerPaint = Paint().apply {
            color = AppMuted.toArgbInt()
            textSize = 24.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        drawContext.canvas.nativeCanvas.drawText(sigText, cx, cy + (centerPaint.textSize / 3f), centerPaint)
    }
}

@Composable
fun PitchBars(
    hist: IntArray,
    keyPc: Int,
    keyMinor: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val peak = hist.maxOrNull() ?: 0
        val ml = 6.dp.toPx()
        val mb = 20.dp.toPx()
        val mt = 6.dp.toPx()
        val bw = (size.width - 2 * ml) / 12f
        val height = size.height - mb - mt

        val scaleSet = if (keyPc >= 0) {
            val ints = if (keyMinor) listOf(0, 2, 3, 5, 7, 8, 10) else listOf(0, 2, 4, 5, 7, 9, 11)
            ints.map { (keyPc + it) % 12 }.toSet()
        } else emptySet()

        val labelPaint = Paint().apply {
            textSize = 11.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        for (pc in 0 until 12) {
            val x = ml + pc * bw
            val col = when {
                pc == keyPc -> AppWarn
                pc in scaleSet -> AppAccent
                else -> AppSurface2
            }

            val count = hist[pc]
            val hh = if (peak > 0) height * count / peak.toFloat() else 0f
            val barHeight = maxOf(hh, 2.dp.toPx())

            drawRoundRect(
                color = col,
                topLeft = Offset(x + bw * 0.15f, size.height - mb - barHeight),
                size = Size(bw * 0.7f, barHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            val textCol = if (pc == keyPc) AppText else AppMuted
            labelPaint.color = textCol.toArgbInt()
            drawContext.canvas.nativeCanvas.drawText(
                MusicTheory.NOTE_NAMES[pc],
                x + bw / 2f,
                size.height - mb * 0.4f,
                labelPaint
            )
        }
    }
}

@Composable
fun BeatDots(
    beats: Int,
    currentBeat: Int,
    accents: List<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val n = maxOf(1, beats)
        val gap = size.width / n
        val r = minOf(size.height * 0.38f, gap * 0.36f)
        val cy = size.height / 2f

        for (k in 0 until n) {
            val x = gap * (k + 0.5f)
            if (k == currentBeat) {
                val col = if (k in accents) AppWarn else AppAccent
                drawCircle(
                    color = col,
                    radius = r,
                    center = Offset(x, cy)
                )
            } else {
                drawCircle(
                    color = AppSurface2,
                    radius = r,
                    center = Offset(x, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
