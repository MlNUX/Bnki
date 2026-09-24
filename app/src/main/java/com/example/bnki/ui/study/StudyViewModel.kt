package com.example.bnki.ui.study

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bnki.data.AppSettings
import com.example.bnki.data.BnkiRepository
import com.example.bnki.data.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class StudyState(
    val loading: Boolean = true,
    val queue: List<Card> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val reviewedCount: Int = 0,
    /** Aktuelle Karte wurde als leer (automatisch falsch) aufgelöst. */
    val autoWrong: Boolean = false,
) {
    val current: Card? get() = queue.getOrNull(index)
    val finished: Boolean get() = !loading && index >= queue.size
}

class StudyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BnkiRepository.from(app)
    private val settings = AppSettings.get(app)
    private var deckId: Long = 0

    val state = MutableStateFlow(StudyState())

    val canvasGrid = settings.canvasGrid
    val answerInputMode = settings.answerInputMode

    fun load(deckId: Long) {
        this.deckId = deckId
        viewModelScope.launch {
            val queue = when {
                // Dev-Modus: immer alle Karten abfragen (ohne Fälligkeit/Limit).
                settings.devMode.value -> repo.buildTestQueue(deckId)
                // deckId == 0 => über alle Stapel hinweg lernen (Startseiten-Button).
                deckId == 0L -> repo.buildStudyQueueAllDecks()
                else -> repo.buildStudyQueue(deckId)
            }
            // Reihenfolge mischen, damit die Abfrage nicht immer gleich läuft.
            state.value = StudyState(loading = false, queue = queue.shuffled())
        }
    }

    /** Karte auflösen. wasBlank=true, wenn nichts geschrieben wurde. */
    fun reveal(wasBlank: Boolean) {
        state.value = state.value.copy(revealed = true, autoWrong = wasBlank)
    }

    /** Bewertung anwenden und zur nächsten Karte. */
    fun grade(quality: Int) {
        val s = state.value
        val card = s.current ?: return
        viewModelScope.launch {
            // Dev-Modus: nur durchblättern, keine echte Bewertung speichern
            // (verfälscht sonst Fälligkeit & Statistik).
            if (!settings.devMode.value) repo.recordReview(card, quality)
            state.value = s.copy(
                index = s.index + 1,
                revealed = false,
                autoWrong = false,
                reviewedCount = s.reviewedCount + 1,
            )
        }
    }
}
