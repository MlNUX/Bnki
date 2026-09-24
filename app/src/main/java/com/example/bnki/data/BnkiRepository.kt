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
    /** Beobachtet die unmittelbaren Unterstapel eines Elternstapels. */
    fun observeDecksWithCounts(
        parentId: Long? = null,
        now: Long = System.currentTimeMillis(),
    ): Flow<List<DeckWithCounts>> =
        deckDao.observeWithCounts(parentId, now).map { rows -> rows.map { it.toDeckWithCounts() } }

    fun observeTotalDue(now: Long = System.currentTimeMillis()): Flow<Int> =
        deckDao.observeTotalDue(now)

    fun observeTotalCards(): Flow<Int> = cardDao.observeTotalCount()

    suspend fun getDeck(id: Long): Deck? = deckDao.getById(id)
    fun observeDeck(id: Long): Flow<Deck?> = deckDao.observeById(id)
    /** Gibt 0 zurück, wenn auf derselben Ebene bereits ein gleichnamiger Stapel existiert. */
    suspend fun upsertDeck(deck: Deck): Long {
        if (deckDao.hasSiblingWithName(deck.parentId, deck.name, deck.id)) return 0L
        return if (deck.id == 0L) deckDao.insert(deck) else { deckDao.update(deck); deck.id }
    }

    /** Erstellt einen Unterstapel nur in einem leeren Stapel oder Ordnerstapel. */
    suspend fun createSubDeck(parentId: Long, name: String): Boolean {
        val parent = deckDao.getById(parentId) ?: return false
        if (parent.type() == DeckContentType.CARDS) return false
        if (deckDao.hasSiblingWithName(parentId, name, 0L)) return false
        if (parent.type() == DeckContentType.EMPTY) {
            deckDao.update(parent.copy(contentType = DeckContentType.SUBDECKS.name))
        }
        deckDao.insert(Deck(name = name, parentId = parentId))
        return true
    }

    /** Löscht einen Stapel mitsamt allen Unterstapeln und aktualisiert den Elternstapel. */
    suspend fun deleteDeck(deck: Deck) {
        val actual = deckDao.getById(deck.id) ?: return
        deleteDeckTree(actual)
        actual.parentId?.let { refreshDeckContent(it) }
    }

    // --- Cards ---
    fun observeCards(deckId: Long): Flow<List<Card>> = cardDao.observeByDeck(deckId)
    suspend fun getCards(deckId: Long): List<Card> = cardDao.getByDeck(deckId)
    suspend fun getCard(id: Long): Card? = cardDao.getById(id)
    /**
     * Neue Karten sind nur in leeren Stapeln oder Kartenstapeln erlaubt.
     * Bei Erfolg wird ein leerer Stapel zum Kartenstapel.
     */
    suspend fun upsertCard(card: Card): Long {
        if (card.id != 0L) {
            cardDao.update(card)
            return card.id
        }
        val deck = deckDao.getById(card.deckId) ?: return 0L
        if (deck.type() == DeckContentType.SUBDECKS) return 0L
        if (deck.type() == DeckContentType.EMPTY) {
            deckDao.update(deck.copy(contentType = DeckContentType.CARDS.name))
        }
        return cardDao.insert(card)
    }

    suspend fun deleteCard(card: Card) {
        cardDao.delete(card)
        refreshDeckContent(card.deckId)
    }

    // --- Lernen ---

    /**
     * Baut die heutige Lern-Queue für ein Deck unter Beachtung der Tageslimits:
     * zuerst fällige Wiederholungen, dann neue Karten (nach Restbudget).
     */
    suspend fun buildStudyQueue(deckId: Long, now: Long = System.currentTimeMillis()): List<Card> {
        val deck = deckDao.getById(deckId) ?: return emptyList()
        // Ordnerstapel lernen rekursiv alle enthaltenen Kartenstapel. Jedes
        // Blatt behält dabei sein eigenes Tageslimit und seine Fälligkeiten.
        val children = deckDao.getChildren(deckId)
        if (children.isNotEmpty()) {
            return children.flatMap { child -> buildStudyQueue(child.id, now) }
        }
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
        if (deckId == 0L) cardDao.getAll() else getCardsInTree(deckId)

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
        var imported = 0
        for (r in rows) {
            if (upsertCard(Card(deckId = deckId, front = r.front, back = r.back, hint = r.hint, tags = r.tags)) != 0L) {
                imported++
            }
        }
        return imported
    }

    // --- Statistik ---
    fun observeLogsSince(since: Long): Flow<List<StudyLog>> = studyLogDao.observeSince(since)

    private suspend fun deleteDeckTree(deck: Deck) {
        deckDao.getChildren(deck.id).forEach { deleteDeckTree(it) }
        deckDao.delete(deck)
    }

    /** Setzt den Typ anhand des tatsächlichen Inhalts zurück bzw. wiederher. */
    private suspend fun refreshDeckContent(deckId: Long) {
        val deck = deckDao.getById(deckId) ?: return
        val type = when {
            deckDao.getChildren(deckId).isNotEmpty() -> DeckContentType.SUBDECKS
            cardDao.countByDeck(deckId) > 0 -> DeckContentType.CARDS
            else -> DeckContentType.EMPTY
        }
        if (deck.contentType != type.name) deckDao.update(deck.copy(contentType = type.name))
    }

    private fun Deck.type(): DeckContentType =
        runCatching { DeckContentType.valueOf(contentType) }.getOrDefault(DeckContentType.EMPTY)

    /** Liefert alle Karten eines Stapels und aller verschachtelten Unterstapel. */
    private suspend fun getCardsInTree(deckId: Long): List<Card> {
        val children = deckDao.getChildren(deckId)
        return if (children.isEmpty()) cardDao.getByDeck(deckId)
        else children.flatMap { child -> getCardsInTree(child.id) }
    }

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
