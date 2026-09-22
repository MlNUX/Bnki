package com.example.bnki

import com.example.bnki.data.Card
import com.example.bnki.domain.Sm2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class Sm2Test {

    private fun newCard() = Card(id = 1, deckId = 1, front = "f", back = "b")

    @Test
    fun firstCorrectReview_setsIntervalToOneDay() {
        val result = Sm2.review(newCard(), Sm2.QUALITY_GOOD, now = 0)
        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(TimeUnit.DAYS.toMillis(1), result.dueDate)
    }

    @Test
    fun secondCorrectReview_setsIntervalToSixDays() {
        val first = Sm2.review(newCard(), Sm2.QUALITY_GOOD, now = 0)
        val second = Sm2.review(first, Sm2.QUALITY_GOOD, now = 0)
        assertEquals(2, second.repetitions)
        assertEquals(6, second.intervalDays)
    }

    @Test
    fun thirdReview_growsByEasiness() {
        var c = newCard()
        c = Sm2.review(c, Sm2.QUALITY_GOOD, now = 0) // interval 1
        c = Sm2.review(c, Sm2.QUALITY_GOOD, now = 0) // interval 6
        c = Sm2.review(c, Sm2.QUALITY_GOOD, now = 0) // interval ~ 6 * easiness
        assertEquals(3, c.repetitions)
        assertTrue("Intervall sollte über 6 liegen", c.intervalDays > 6)
    }

    @Test
    fun wrongAnswer_resetsRepetitionsAndInterval() {
        var c = newCard()
        c = Sm2.review(c, Sm2.QUALITY_GOOD, now = 0)
        c = Sm2.review(c, Sm2.QUALITY_GOOD, now = 0) // interval 6, reps 2
        val wrong = Sm2.review(c, Sm2.QUALITY_AGAIN, now = 0)
        assertEquals(0, wrong.repetitions)
        assertEquals(1, wrong.intervalDays)
    }

    @Test
    fun blankAnswer_isTreatedAsWrong() {
        val result = Sm2.review(newCard(), Sm2.QUALITY_BLANK, now = 0)
        assertEquals(0, result.repetitions)
        assertEquals(1, result.intervalDays)
    }

    @Test
    fun easiness_neverDropsBelowMinimum() {
        var c = newCard()
        repeat(10) { c = Sm2.review(c, Sm2.QUALITY_BLANK, now = 0) }
        assertTrue(c.easiness >= Sm2.MIN_EASINESS)
    }

    @Test
    fun easyAnswer_raisesEasiness() {
        val result = Sm2.review(newCard(), Sm2.QUALITY_EASY, now = 0)
        assertTrue(result.easiness > 2.5)
    }
}
