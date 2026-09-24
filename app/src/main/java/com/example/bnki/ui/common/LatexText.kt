package com.example.bnki.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import ru.noties.jlatexmath.JLatexMathDrawable

/**
 * Zeigt Kartentext mit eingebetteten LaTeX-Formeln ($...$ inline, $$...$$ abgesetzt)
 * und Markdown-Codeblöcken (```sprache ... ```).
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
    val blocks = remember(text) { parseCardText(text) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is CardTextBlock.Plain -> LatexTextSegment(
                    text = block.text,
                    color = color,
                    fontSize = fontSize,
                )
                is CardTextBlock.Code -> CodeBlock(
                    code = block.code,
                    language = block.language,
                    fontSize = fontSize,
                )
            }
        }
    }
}

@Composable
private fun LatexTextSegment(
    text: String,
    color: Color,
    fontSize: TextUnit,
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
        color = color,
        fontSize = fontSize,
        inlineContent = inlineContent,
    )
}

@Composable
private fun CodeBlock(code: String, language: String?, fontSize: TextUnit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(12.dp)) {
            if (language != null) {
                Text(
                    text = language,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            SelectionContainer {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}

internal sealed interface CardTextBlock {
    data class Plain(val text: String) : CardTextBlock
    data class Code(val code: String, val language: String?) : CardTextBlock
}

// Fences müssen jeweils allein auf ihrer Zeile stehen. Nicht geschlossene
// Fences bleiben bewusst normaler Text, damit die Eingabe nicht verschwindet.
private val CODE_BLOCK_REGEX = Regex(
    """(?m)^[\t ]*```([A-Za-z0-9_+.-]*)[\t ]*\r?\n([\s\S]*?)^[\t ]*```[\t ]*(?:\r?\n|$)""",
)

internal fun parseCardText(text: String): List<CardTextBlock> {
    val blocks = mutableListOf<CardTextBlock>()
    var last = 0

    for (match in CODE_BLOCK_REGEX.findAll(text)) {
        // Die Zeilenumbrüche, die nur die Markdown-Fence abgrenzen, brauchen
        // wir nicht zusätzlich zum Abstand zwischen den Compose-Blöcken.
        val plain = text.substring(last, match.range.first).removeSuffix("\r\n").removeSuffix("\n")
        if (plain.isNotEmpty()) blocks += CardTextBlock.Plain(plain)

        val language = match.groupValues[1].ifBlank { null }
        val code = match.groupValues[2].removeSuffix("\r\n").removeSuffix("\n")
        blocks += CardTextBlock.Code(code = code, language = language)
        last = match.range.last + 1
    }

    val trailing = text.substring(last).removePrefix("\r\n").removePrefix("\n")
    if (trailing.isNotEmpty()) blocks += CardTextBlock.Plain(trailing)

    return blocks.ifEmpty { listOf(CardTextBlock.Plain(text)) }
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
