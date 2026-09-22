package com.example.bnki.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Deck::class, Card::class, StudyLog::class],
    version = 1,
    exportSchema = false,
)
abstract class BnkiDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun studyLogDao(): StudyLogDao

    companion object {
        @Volatile
        private var INSTANCE: BnkiDatabase? = null

        fun get(context: Context): BnkiDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BnkiDatabase::class.java,
                    "bnki.db",
                ).build().also { INSTANCE = it }
            }
    }
}
