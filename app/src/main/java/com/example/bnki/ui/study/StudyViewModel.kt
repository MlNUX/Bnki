package com.example.bnki.ui.study

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    private var deckId: Long = 0

    val state = MutableStateFlow(StudyState())

    fun load(deckId: Long) {
        this.deckId = deckId
        viewModelScope.launch {
            // deckId == 0 => über alle Stapel hinweg lernen (Startseiten-Button).
            val queue = if (deckId == 0L) repo.buildStudyQueueAllDecks()
            else repo.buildStudyQueue(deckId)
            state.value = StudyState(loading = false, queue = queue)
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
            repo.recordReview(card, quality)
            state.value = s.copy(
                index = s.index + 1,
                revealed = false,
                autoWrong = false,
                reviewedCount = s.reviewedCount + 1,
            )
        }
    }
}
