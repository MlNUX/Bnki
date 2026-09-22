package com.example.bnki.domain

import com.example.bnki.data.Card
import java.util.concurrent.TimeUnit

/**
 * SM-2 Spaced-Repetition-Algorithmus (Prinzip hinter Anki).
 *
 * quality: 0..5 (Selbstbewertung). < 3 = falsch/nicht gewusst.
 * Mapping der 3 UI-Buttons:
 *   Nochmal -> 2, Gut -> 4, Leicht -> 5
 * Leere Handschrift-Antwort -> 0 (automatisch falsch).
 */
object Sm2 {
    const val QUALITY_AGAIN = 2 // "Nochmal"
    const val QUALITY_GOOD = 4  // "Gut"
    const val QUALITY_EASY = 5  // "Leicht"
    const val QUALITY_BLANK = 0 // leere Antwort

    const val MIN_EASINESS = 1.3

    /**
     * Wendet eine Bewertung auf die Karte an und gibt die aktualisierte Karte
     * (neues Intervall, Easiness, repetitions, dueDate) zurück.
     */
    fun review(card: Card, quality: Int, now: Long = System.currentTimeMillis()): Card {
        val q = quality.coerceIn(0, 5)

        // Easiness anpassen (Standard-SM-2-Formel), Untergrenze 1.3.
        val newEasiness = (card.easiness + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
            .coerceAtLeast(MIN_EASINESS)

        val newRepetitions: Int
        val newInterval: Int
        if (q < 3) {
            // Falsch -> von vorne, morgen erneut.
            newRepetitions = 0
            newInterval = 1
        } else {
            newRepetitions = card.repetitions + 1
            newInterval = when (newRepetitions) {
                1 -> 1
                2 -> 6
                else -> Math.round(card.intervalDays * newEasiness).toInt().coerceAtLeast(1)
            }
        }

        val newDue = now + TimeUnit.DAYS.toMillis(newInterval.toLong())

        return card.copy(
            easiness = newEasiness,
            repetitions = newRepetitions,
            intervalDays = newInterval,
            dueDate = newDue,
        )
    }
}
