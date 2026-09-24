package com.example.bnki.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deck: Deck): Long

    @Update
    suspend fun update(deck: Deck)

    @Delete
    suspend fun delete(deck: Deck)

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getById(id: Long): Deck?

    @Query("SELECT * FROM decks WHERE id = :id")
    fun observeById(id: Long): Flow<Deck?>

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Deck>>

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Deck>

    @Query("SELECT * FROM decks WHERE parentId = :parentId ORDER BY name COLLATE NOCASE")
    suspend fun getChildren(parentId: Long): List<Deck>

    /** Namen müssen unter demselben Elternstapel eindeutig sein (ohne Groß-/Kleinschreibung). */
    @Query(
        """
        SELECT COUNT(*) > 0 FROM decks
        WHERE parentId IS :parentId
          AND name = :name COLLATE NOCASE
          AND id != :excludedId
        """
    )
    suspend fun hasSiblingWithName(parentId: Long?, name: String, excludedId: Long): Boolean

    /** Decks mit Gesamt- und Fälligkeitszahl für die Übersicht. */
    @Query(
        """
        SELECT d.*,
            (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id) AS totalCards,
            (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.dueDate <= :now) AS dueCards,
            (SELECT COUNT(*) FROM decks child WHERE child.parentId = d.id) AS subDeckCount
        FROM decks d
        WHERE d.parentId IS :parentId
        ORDER BY d.name COLLATE NOCASE
        """
    )
    fun observeWithCounts(parentId: Long?, now: Long): Flow<List<DeckCountsRow>>

    @Query("SELECT COUNT(*) FROM cards WHERE dueDate <= :now")
    fun observeTotalDue(now: Long): Flow<Int>
}

/** Flache Zeile für die JOIN-Abfrage; wird ins DeckWithCounts gemappt. */
data class DeckCountsRow(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val contentType: String,
    val createdAt: Long,
    val newCardsPerDay: Int,
    val maxReviewsPerDay: Int,
    val totalCards: Int,
    val dueCards: Int,
    val subDeckCount: Int,
) {
    fun toDeckWithCounts() = DeckWithCounts(
        deck = Deck(
            id = id,
            name = name,
            parentId = parentId,
            contentType = contentType,
            createdAt = createdAt,
            newCardsPerDay = newCardsPerDay,
            maxReviewsPerDay = maxReviewsPerDay,
        ),
        totalCards = totalCards,
        dueCards = dueCards,
        subDeckCount = subDeckCount,
    )
}

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: Card): Long

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): Card?

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt")
    fun observeByDeck(deckId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId")
    suspend fun getByDeck(deckId: Long): List<Card>

    @Query("SELECT * FROM cards ORDER BY deckId, createdAt")
    suspend fun getAll(): List<Card>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    suspend fun countByDeck(deckId: Long): Int

    @Query("SELECT COUNT(*) FROM cards")
    fun observeTotalCount(): Flow<Int>

    /** Fällige, bereits gelernte Karten (repetitions > 0), älteste zuerst. */
    @Query(
        """
        SELECT * FROM cards
        WHERE deckId = :deckId AND dueDate <= :now AND repetitions > 0
        ORDER BY dueDate
        LIMIT :limit
        """
    )
    suspend fun getDueReviews(deckId: Long, now: Long, limit: Int): List<Card>

    @Query("SELECT COUNT(*) FROM cards WHERE dueDate <= :now")
    suspend fun countDue(now: Long): Int

    /** Neue, noch nie gelernte Karten (repetitions = 0). */
    @Query(
        """
        SELECT * FROM cards
        WHERE deckId = :deckId AND repetitions = 0
        ORDER BY createdAt
        LIMIT :limit
        """
    )
    suspend fun getNewCards(deckId: Long, limit: Int): List<Card>
}

@Dao
interface StudyLogDao {
    @Insert
    suspend fun insert(log: StudyLog): Long

    /** Wie viele neue Karten wurden seit :since in diesem Deck gelernt? */
    @Query(
        "SELECT COUNT(*) FROM study_logs WHERE deckId = :deckId AND wasNew = 1 AND reviewedAt >= :since"
    )
    suspend fun countNewSince(deckId: Long, since: Long): Int

    /** Wie viele Wiederholungen (nicht-neu) seit :since in diesem Deck? */
    @Query(
        "SELECT COUNT(*) FROM study_logs WHERE deckId = :deckId AND wasNew = 0 AND reviewedAt >= :since"
    )
    suspend fun countReviewsSince(deckId: Long, since: Long): Int

    @Query("SELECT * FROM study_logs WHERE reviewedAt >= :since ORDER BY reviewedAt")
    fun observeSince(since: Long): Flow<List<StudyLog>>

    @Query("SELECT COUNT(*) FROM study_logs")
    suspend fun count(): Int
}
