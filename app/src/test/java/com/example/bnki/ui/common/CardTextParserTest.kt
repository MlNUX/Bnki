package com.example.bnki.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CardTextParserTest {
    @Test
    fun `splits fenced code from surrounding text`() {
        val result = parseCardText(
            """Vor dem Beispiel
            |```kotlin
            |val answer = 42
            |```
            |Danach
            """.trimMargin(),
        )

        assertEquals(
            listOf(
                CardTextBlock.Plain("Vor dem Beispiel"),
                CardTextBlock.Code(code = "val answer = 42", language = "kotlin"),
                CardTextBlock.Plain("Danach"),
            ),
            result,
        )
    }

    @Test
    fun `keeps an unclosed fence as text`() {
        val input = "```kotlin\nval answer = 42"

        assertEquals(listOf(CardTextBlock.Plain(input)), parseCardText(input))
    }
}
