package com.example.bnki.data

import android.content.Context
import com.example.bnki.domain.Sm2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Kapselt die DAOs und die Lernlogik (Tageslimit, Bewertung).
 */
class BnkiRepository(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val studyLogDao: StudyLogDao,
) {
    // --- Decks ---
    fun observeDecksWithCounts(now: Long = System.currentTimeMillis()): Flow<List<DeckWithCounts>> =
        deckDao.observeWithCounts(now).map { rows -> rows.map { it.toDeckWithCounts() } }

    fun observeTotalDue(now: Long = System.currentTimeMillis()): Flow<Int> =
        deckDao.observeTotalDue(now)

    suspend fun getDeck(id: Long): Deck? = deckDao.getById(id)
    suspend fun upsertDeck(deck: Deck): Long =
        if (deck.id == 0L) deckDao.insert(deck) else { deckDao.update(deck); deck.id }
    suspend fun deleteDeck(deck: Deck) = deckDao.delete(deck)

    // --- Cards ---
    fun observeCards(deckId: Long): Flow<List<Card>> = cardDao.observeByDeck(deckId)
    suspend fun getCards(deckId: Long): List<Card> = cardDao.getByDeck(deckId)
    suspend fun getCard(id: Long): Card? = cardDao.getById(id)
    suspend fun upsertCard(card: Card): Long =
        if (card.id == 0L) cardDao.insert(card) else { cardDao.update(card); card.id }
    suspend fun deleteCard(card: Card) = cardDao.delete(card)

    // --- Lernen ---

    /**
     * Baut die heutige Lern-Queue für ein Deck unter Beachtung der Tageslimits:
     * zuerst fällige Wiederholungen, dann neue Karten (nach Restbudget).
     */
    suspend fun buildStudyQueue(deckId: Long, now: Long = System.currentTimeMillis()): List<Card> {
        val deck = deckDao.getById(deckId) ?: return emptyList()
        val since = startOfDay(now)

        val reviewsDone = studyLogDao.countReviewsSince(deckId, since)
        val newDone = studyLogDao.countNewSince(deckId, since)

        val reviewBudget = if (deck.maxReviewsPerDay <= 0) Int.MAX_VALUE
        else (deck.maxReviewsPerDay - reviewsDone).coerceAtLeast(0)
        val newBudget = (deck.newCardsPerDay - newDone).coerceAtLeast(0)

        val due = if (reviewBudget > 0) cardDao.getDueReviews(deckId, now, reviewBudget) else emptyList()
        val fresh = if (newBudget > 0) cardDao.getNewCards(deckId, newBudget) else emptyList()
        return due + fresh
    }

    /**
     * Baut die heutige Lern-Queue über **alle** Stapel hinweg (jeweils unter
     * Beachtung der Deck-Tageslimits).
     */
    suspend fun buildStudyQueueAllDecks(now: Long = System.currentTimeMillis()): List<Card> =
        deckDao.getAll().flatMap { deck -> buildStudyQueue(deck.id, now) }

    /**
     * Dev-Modus: alle Karten (eines Decks, oder deckId==0 = alle Decks),
     * unabhängig von Fälligkeit und Tageslimit – zum Testen der Abfrage.
     */
    suspend fun buildTestQueue(deckId: Long): List<Card> =
        if (deckId == 0L) cardDao.getAll() else cardDao.getByDeck(deckId)

    /** Wendet eine Bewertung an, speichert die Karte und protokolliert sie. */
    suspend fun recordReview(card: Card, quality: Int, now: Long = System.currentTimeMillis()) {
        val wasNew = card.repetitions == 0
        val updated = Sm2.review(card, quality, now)
        cardDao.update(updated)
        studyLogDao.insert(
            StudyLog(
                deckId = card.deckId,
                cardId = card.id,
                reviewedAt = now,
                quality = quality,
                wasNew = wasNew,
            )
        )
    }

    suspend fun countAllDue(now: Long = System.currentTimeMillis()): Int = cardDao.countDue(now)

    // --- Import / Export ---
    suspend fun exportDeckCsv(deckId: Long): String = CsvIo.export(cardDao.getByDeck(deckId))

    /** Importiert CSV-Zeilen als neue Karten in ein Deck. Gibt die Anzahl zurück. */
    suspend fun importCsv(deckId: Long, csv: String): Int {
        val rows = CsvIo.parse(csv).filter { it.front.isNotBlank() && it.back.isNotBlank() }
        for (r in rows) {
            cardDao.insert(
                Card(deckId = deckId, front = r.front, back = r.back, hint = r.hint, tags = r.tags)
            )
        }
        return rows.size
    }

    // --- Statistik ---
    fun observeLogsSince(since: Long): Flow<List<StudyLog>> = studyLogDao.observeSince(since)

    companion object {
        fun startOfDay(now: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun from(context: Context): BnkiRepository {
            val db = BnkiDatabase.get(context)
            return BnkiRepository(db.deckDao(), db.cardDao(), db.studyLogDao())
        }
    }
}
