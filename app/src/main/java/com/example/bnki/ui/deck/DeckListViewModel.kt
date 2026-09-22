package com.example.bnki.ui.deck

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bnki.data.AppSettings
import com.example.bnki.data.BnkiRepository
import com.example.bnki.data.Deck
import com.example.bnki.data.DeckWithCounts
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeckListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BnkiRepository.from(app)

    val decks: StateFlow<List<DeckWithCounts>> =
        repo.observeDecksWithCounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val devMode: StateFlow<Boolean> = AppSettings.get(app).devMode

    fun addDeck(name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) repo.upsertDeck(Deck(name = trimmed))
    }

    fun deleteDeck(deck: Deck) = viewModelScope.launch {
        repo.deleteDeck(deck)
    }
}
