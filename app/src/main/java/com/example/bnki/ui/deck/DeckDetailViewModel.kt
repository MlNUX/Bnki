package com.example.bnki.ui.deck

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bnki.data.BnkiRepository
import com.example.bnki.data.Card
import com.example.bnki.data.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeckDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BnkiRepository.from(app)
    private val deckId = MutableStateFlow(0L)

    val deck = MutableStateFlow<Deck?>(null)

    @Suppress("OPT_IN_USAGE")
    val cards: StateFlow<List<Card>> =
        deckId.flatMapLatest { id -> repo.observeCards(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(id: Long) {
        deckId.value = id
        viewModelScope.launch { deck.value = repo.getDeck(id) }
    }

    fun deleteCard(card: Card) = viewModelScope.launch { repo.deleteCard(card) }

    fun importCsv(uri: Uri, onDone: (Int) -> Unit) = viewModelScope.launch {
        val text = runCatching {
            getApplication<Application>().contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        val n = if (text != null) repo.importCsv(deckId.value, text) else 0
        onDone(n)
    }
}
