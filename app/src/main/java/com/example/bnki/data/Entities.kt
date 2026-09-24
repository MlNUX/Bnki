package com.example.bnki.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Ein Stapel ist leer, enthält Karten oder dient als Ordner für Unterstapel. */
enum class DeckContentType { EMPTY, CARDS, SUBDECKS }

@Entity(tableName = "decks", indices = [Index("parentId")])
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** `null` bedeutet, dass der Stapel auf der obersten Ebene liegt. */
    val parentId: Long? = null,
    /** Persistiert als String, um eine zusätzliche Room-Typkonvertierung zu vermeiden. */
    val contentType: String = DeckContentType.EMPTY.name,
    val createdAt: Long = System.currentTimeMillis(),
    /** Tägliches Limit an neuen Karten. */
    val newCardsPerDay: Int = 20,
    /** Tägliches Limit an Wiederholungen (0 = unbegrenzt). */
    val maxReviewsPerDay: Int = 100,
)

/**
 * Eine Karteikarte. front/back/hint dürfen Text mit LaTeX ($...$) und
 * Markdown-Codeblöcken (```...```) enthalten.
 * Die SM-2-Felder steuern die Wiederholung.
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deckId")],
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val hint: String = "",
    val tags: String = "",
    /** Optionale Bilder (Dateinamen im App-Speicher), leer = keins. */
    val frontImage: String = "",
    val backImage: String = "",
    // SM-2-Zustand
    val intervalDays: Int = 0,
    val easiness: Double = 2.5,
    val repetitions: Int = 0,
    /** Fällig ab diesem Zeitpunkt (Epoch-Millis). Neue Karten: sofort. */
    val dueDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
)

/** Protokoll jeder Bewertung – Basis für Tageslimit und Statistik. */
@Entity(
    tableName = "study_logs",
    indices = [Index("deckId"), Index("reviewedAt")],
)
data class StudyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val cardId: Long,
    val reviewedAt: Long = System.currentTimeMillis(),
    /** SM-2-Qualität 0..5. */
    val quality: Int,
    /** War die Karte zu diesem Zeitpunkt neu (erste Bewertung)? */
    val wasNew: Boolean,
)

/** Deck inkl. abgeleiteter Zähler für die Übersichtsliste. */
data class DeckWithCounts(
    val deck: Deck,
    val totalCards: Int,
    val dueCards: Int,
    val subDeckCount: Int,
)
