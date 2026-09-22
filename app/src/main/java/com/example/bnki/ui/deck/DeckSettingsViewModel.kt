package com.example.bnki.ui.deck

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bnki.data.BnkiRepository
import com.example.bnki.data.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class DeckSettingsState(
    val name: String = "",
    val newCardsPerDay: String = "20",
    val maxReviewsPerDay: String = "100",
    val loaded: Boolean = false,
)

class DeckSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BnkiRepository.from(app)
    private var deck: Deck? = null

    val state = MutableStateFlow(DeckSettingsState())

    fun load(deckId: Long) = viewModelScope.launch {
        repo.getDeck(deckId)?.let { d ->
            deck = d
            state.value = DeckSettingsState(
                name = d.name,
                newCardsPerDay = d.newCardsPerDay.toString(),
                maxReviewsPerDay = d.maxReviewsPerDay.toString(),
                loaded = true,
            )
        }
    }

    fun onName(v: String) { state.value = state.value.copy(name = v) }
    fun onNew(v: String) { state.value = state.value.copy(newCardsPerDay = v.filter { it.isDigit() }) }
    fun onReviews(v: String) { state.value = state.value.copy(maxReviewsPerDay = v.filter { it.isDigit() }) }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val d = deck ?: return@launch
        val s = state.value
        repo.upsertDeck(
            d.copy(
                name = s.name.trim().ifEmpty { d.name },
                newCardsPerDay = s.newCardsPerDay.toIntOrNull() ?: d.newCardsPerDay,
                maxReviewsPerDay = s.maxReviewsPerDay.toIntOrNull() ?: d.maxReviewsPerDay,
            )
        )
        onDone()
    }

    fun deleteDeck(onDone: () -> Unit) = viewModelScope.launch {
        deck?.let { repo.deleteDeck(it) }
        onDone()
    }

    val suggestedFileName: String
        get() = (deck?.name ?: "stapel").replace(Regex("[^A-Za-z0-9_-]"), "_") + ".csv"

    fun exportTo(uri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val d = deck
        if (d == null) { onDone(false); return@launch }
        val csv = repo.exportDeckCsv(d.id)
        val ok = runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                out.write(csv.toByteArray(Charsets.UTF_8))
            }
        }.isSuccess
        onDone(ok)
    }
}
