package com.example.bnki.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import ru.noties.jlatexmath.JLatexMathDrawable

/**
 * Zeigt Text mit eingebetteten LaTeX-Formeln ($...$ inline, $$...$$ abgesetzt).
 *
 * Rendert die Formeln direkt mit JLatexMath (nativ, offline) als Bild und
 * bettet sie über Compose-`inlineContent` in den Text ein – unabhängig vom
 * (fragilen) Markdown-/Inline-Parser, damit auch inline `$...$` zuverlässig
 * gerendert wird.
 */
@Composable
fun LatexText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
) {
    val density = LocalDensity.current
    val textSizePx = with(density) {
        if (fontSize.type == TextUnitType.Sp && fontSize.value > 0f) fontSize.toPx() else 20.sp.toPx()
    }
    val colorArgb = color.toArgb()

    val parsed = remember(text, textSizePx, colorArgb) {
        parseLatex(text, textSizePx, colorArgb, density)
    }

    val inlineContent = remember(parsed) {
        parsed.formulas.mapValues { (_, f) ->
            InlineTextContent(
                Placeholder(
                    width = f.widthSp.sp,
                    height = f.heightSp.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                Image(bitmap = f.image, contentDescription = f.latex)
            }
        }
    }

    Text(
        text = parsed.annotated,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        inlineContent = inlineContent,
    )
}

private class ParsedLatex(
    val annotated: AnnotatedString,
    val formulas: Map<String, Formula>,
)

private class Formula(
    val latex: String,
    val image: ImageBitmap,
    val widthSp: Float,
    val heightSp: Float,
)

// $$...$$ (abgesetzt, darf mehrzeilig sein) oder $...$ (inline).
private val LATEX_REGEX = Regex("""\$\$(.+?)\$\$|\$(.+?)\$""", RegexOption.DOT_MATCHES_ALL)

private fun parseLatex(
    text: String,
    textSizePx: Float,
    colorArgb: Int,
    density: Density,
): ParsedLatex {
    val builder = AnnotatedString.Builder()
    val formulas = LinkedHashMap<String, Formula>()
    var last = 0
    var idx = 0

    for (m in LATEX_REGEX.findAll(text)) {
        builder.append(text.substring(last, m.range.first))
        val latex = m.groupValues[1].ifEmpty { m.groupValues[2] }
        val bmp = renderFormula(latex, textSizePx, colorArgb)
        if (bmp != null) {
            val id = "latex$idx"
            idx++
            val pxToSp = density.density * density.fontScale
            formulas[id] = Formula(
                latex = latex,
                image = bmp.asImageBitmap(),
                widthSp = bmp.width / pxToSp,
                heightSp = bmp.height / pxToSp,
            )
            builder.appendInlineContent(id, latex)
        } else {
            // Ungültige Formel: Rohtext (inkl. $) anzeigen, statt eine Lücke.
            builder.append(m.value)
        }
        last = m.range.last + 1
    }
    builder.append(text.substring(last))

    return ParsedLatex(builder.toAnnotatedString(), formulas)
}

private fun renderFormula(latex: String, textSizePx: Float, colorArgb: Int): Bitmap? = try {
    val drawable = JLatexMathDrawable.builder(latex)
        .textSize(textSizePx)
        .color(colorArgb)
        .align(JLatexMathDrawable.ALIGN_LEFT)
        .build()
    val w = drawable.intrinsicWidth.coerceAtLeast(1)
    val h = drawable.intrinsicHeight.coerceAtLeast(1)
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
        drawable.setBounds(0, 0, w, h)
        drawable.draw(Canvas(bmp))
    }
} catch (e: Exception) {
    null
}
