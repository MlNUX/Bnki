package com.example.bnki.ui.card

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bnki.data.BnkiRepository
import com.example.bnki.data.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class CardEditState(
    val front: String = "",
    val back: String = "",
    val hint: String = "",
    val tags: String = "",
)

class CardEditViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BnkiRepository.from(app)

    private var deckId: Long = 0
    private var cardId: Long = 0
    private var existing: Card? = null

    val state = MutableStateFlow(CardEditState())

    fun load(deckId: Long, cardId: Long) {
        this.deckId = deckId
        this.cardId = cardId
        if (cardId != 0L) viewModelScope.launch {
            repo.getCard(cardId)?.let { c ->
                existing = c
                state.value = CardEditState(c.front, c.back, c.hint, c.tags)
            }
        }
    }

    fun onFront(v: String) { state.value = state.value.copy(front = v) }
    fun onBack(v: String) { state.value = state.value.copy(back = v) }
    fun onHint(v: String) { state.value = state.value.copy(hint = v) }
    fun onTags(v: String) { state.value = state.value.copy(tags = v) }

    val canSave: Boolean
        get() = state.value.front.isNotBlank() && state.value.back.isNotBlank()

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = state.value
        val card = (existing ?: Card(deckId = deckId, front = "", back = "")).copy(
            front = s.front.trim(),
            back = s.back.trim(),
            hint = s.hint.trim(),
            tags = s.tags.trim(),
        )
        repo.upsertCard(card)
        onDone()
    }
}
