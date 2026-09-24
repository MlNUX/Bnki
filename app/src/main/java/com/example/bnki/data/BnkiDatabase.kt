package com.example.bnki.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Deck::class, Card::class, StudyLog::class],
    version = 3,
    exportSchema = false,
)
abstract class BnkiDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun studyLogDao(): StudyLogDao

    companion object {
        @Volatile
        private var INSTANCE: BnkiDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN frontImage TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN backImage TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE decks ADD COLUMN parentId INTEGER")
                db.execSQL("ALTER TABLE decks ADD COLUMN contentType TEXT NOT NULL DEFAULT 'EMPTY'")
                // Alle bisherigen Stapel enthalten ausschließlich Karten.
                db.execSQL(
                    "UPDATE decks SET contentType = 'CARDS' " +
                        "WHERE id IN (SELECT DISTINCT deckId FROM cards)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_decks_parentId ON decks(parentId)")
            }
        }

        fun get(context: Context): BnkiDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BnkiDatabase::class.java,
                    "bnki.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
