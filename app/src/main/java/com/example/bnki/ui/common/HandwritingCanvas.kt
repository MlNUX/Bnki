package com.example.bnki.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPressed
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/** Ein Strich = Liste von Punkten (in Canvas-/Inhaltskoordinaten). */
typealias StrokePoints = SnapshotStateList<Offset>

/** Hält die gezeichneten Striche + Zoom/Verschiebung und bietet Löschen/Rückgängig. */
class HandwritingState {
    val strokes: SnapshotStateList<StrokePoints> = mutableStateListOf()

    /** Zoomfaktor und Verschiebung: screen = inhalt * scale + offset. */
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    /** Ist eine Stift-Seitentaste gedrückt? (aus rohem MotionEvent.buttonState) */
    var buttonPressed by mutableStateOf(false)

    val isEmpty: Boolean get() = strokes.isEmpty()

    fun clear() = strokes.clear()

    /** Zoom/Verschiebung zurücksetzen (Inhalt bleibt). */
    fun resetView() {
        scale = 1f
        offset = Offset.Zero
    }

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
    }

    /** Bildschirmpunkt -> Inhaltskoordinate (Inverse der Anzeige-Transformation). */
    fun toContent(screen: Offset): Offset = (screen - offset) / scale
}

@Composable
fun rememberHandwritingState(): HandwritingState = remember { HandwritingState() }

/**
 * Zeichenfläche, auf der NUR mit dem Stift (Stylus) geschrieben werden kann.
 * Finger-/Handballen-Berührungen werden für die Eingabe ignoriert (Palm-Rejection),
 * dienen aber zum **Zoomen (Pinch) und Verschieben** des Inhalts.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HandwritingCanvas(
    state: HandwritingState,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color(0xFF1565C0),
    strokeWidth: Float = 6f,
    grid: Boolean = false,
    debug: Boolean = false,
) {
    val gridColor = Color(0x33888888)
  Box(modifier) {
    var debugInfo by remember { mutableStateOf("Diagnose: Stift bewegen…") }

    Canvas(
        modifier = Modifier
            .matchParentSize()
            .pointerHoverIcon(PointerIcon.Crosshair)
            .pointerInteropFilter { me ->
                // Button-Zustand direkt aus dem rohen MotionEvent lesen –
                // Compose gibt buttonState nicht zuverlässig an event.buttons weiter.
                state.buttonPressed = me.buttonState != 0
                if (debug) {
                    debugInfo = "tool=${me.getToolType(0)} " +
                        "buttonState=0x${Integer.toHexString(me.buttonState)} " +
                        "actionButton=0x${Integer.toHexString(me.actionButton)} " +
                        "action=${me.actionMasked}"
                }
                false // nicht konsumieren – Zeichnen läuft weiter
            }
            .border(1.dp, Color.LightGray)
            .clipToBounds()
            .pointerInput(strokeWidth) {
                val eraseRadius = strokeWidth * 4f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Stylus ODER Eraser (Samsung meldet die S-Pen-Taste teils als
                    // TOOL_TYPE_ERASER) => Schreiben/Radieren. Sonst Finger => Pan/Zoom.
                    if (down.type == PointerType.Stylus || down.type == PointerType.Eraser) {
                        handleStylus(state, down, eraseRadius)
                    } else {
                        handlePanZoom(state, down)
                    }
                }
            },
    ) {
        withTransform({
            translate(state.offset.x, state.offset.y)
            scale(state.scale, state.scale, pivot = Offset.Zero)
        }) {
            if (grid) {
                val step = 24.dp.toPx()
                // Sichtbarer Bereich in Inhaltskoordinaten.
                val left = -state.offset.x / state.scale
                val top = -state.offset.y / state.scale
                val right = (size.width - state.offset.x) / state.scale
                val bottom = (size.height - state.offset.y) / state.scale
                val lineW = 1f / state.scale // bleibt am Bildschirm ~1px dünn
                var x = floor(left / step) * step
                while (x <= right) {
                    drawLine(gridColor, Offset(x, top), Offset(x, bottom), lineW)
                    x += step
                }
                var y = floor(top / step) * step
                while (y <= bottom) {
                    drawLine(gridColor, Offset(left, y), Offset(right, y), lineW)
                    y += step
                }
            }
            for (points in state.strokes) {
                if (points.size == 1) {
                    drawCircle(strokeColor, radius = strokeWidth / 2f, center = points.first())
                } else if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }

    if (debug) {
        Text(
            text = debugInfo,
            color = Color.Red,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color(0xCCFFFFFF))
                .padding(4.dp),
        )
    }
  }
}

/** Zeichnen bzw. Radieren (S-Pen-Seitentaste) – arbeitet in Inhaltskoordinaten. */
private suspend fun AwaitPointerEventScope.handleStylus(
    state: HandwritingState,
    down: PointerInputChange,
    eraseRadius: Float,
) {
    down.consume()
    var current: StrokePoints? = null
    var everErasing = false

    fun eraseAt(pos: Offset) {
        val hit = state.strokes.filter { pts ->
            pts.any { (it - pos).getDistance() <= eraseRadius }
        }
        if (hit.isNotEmpty()) state.strokes.removeAll(hit)
    }

    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: break
        val isStylusLike = change.type == PointerType.Stylus || change.type == PointerType.Eraser
        // Radieren, wenn: Tool-Typ Eraser ODER Stift-Seitentaste gedrückt
        // (buttonState aus dem rohen MotionEvent, via pointerInteropFilter).
        val erasing = change.type == PointerType.Eraser ||
            state.buttonPressed ||
            event.buttons.isSecondaryPressed ||
            event.buttons.isPressed(5) ||
            event.buttons.isPressed(6)
        if (isStylusLike && change.pressed) {
            val contentPos = state.toContent(change.position)
            if (erasing) {
                everErasing = true
                current = null
                eraseAt(contentPos)
            } else {
                val stroke = current ?: mutableStateListOf<Offset>().also {
                    state.strokes.add(it)
                    current = it
                }
                stroke.add(contentPos)
            }
            change.consume()
        }
        if (!change.pressed) break
    }

    if (current == null && !everErasing && down.type == PointerType.Stylus) {
        state.strokes.add(mutableStateListOf(state.toContent(down.position)))
    }
}

/** Finger-Gesten: Pinch-Zoom + Verschieben des Inhalts (zoomt um den Finger-Mittelpunkt). */
private suspend fun AwaitPointerEventScope.handlePanZoom(
    state: HandwritingState,
    down: PointerInputChange,
) {
    down.consume()
    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.isEmpty()) break

        val zoom = event.calculateZoom()
        val pan = event.calculatePan()
        val centroid = event.calculateCentroid(useCurrent = true)

        if (zoom != 1f || pan != Offset.Zero) {
            val newScale = (state.scale * zoom).coerceIn(0.5f, 6f)
            val effZoom = newScale / state.scale
            // Inhaltspunkt unter dem Finger-Mittelpunkt bleibt fix, plus Verschiebung.
            state.offset = centroid + pan - (centroid - state.offset) * effZoom
            state.scale = newScale
            event.changes.forEach { if (it.pressed) it.consume() }
        }
    }
}
