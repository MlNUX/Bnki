package com.example.bnki.ui.deck

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bnki.data.Card as CardEntity
import com.example.bnki.data.DeckContentType
import com.example.bnki.data.DeckWithCounts
import com.example.bnki.ui.common.LatexText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId: Long,
    onBack: () -> Unit,
    onOpenDeck: (Long) -> Unit,
    onStudy: (Long) -> Unit,
    onAddCard: (Long) -> Unit,
    onEditCard: (Long, Long) -> Unit,
    onSettings: (Long) -> Unit,
    vm: DeckDetailViewModel = viewModel(),
) {
    LaunchedEffect(deckId) { vm.load(deckId) }
    val deck by vm.deck.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    val subDecks by vm.subDecks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddChoice by remember { mutableStateOf(false) }
    var showAddSubDeck by remember { mutableStateOf(false) }
    val contentType = deck?.contentType?.toDeckContentType()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importCsv(uri) { n ->
            Toast.makeText(context, "$n Karten importiert", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deck?.name ?: "Stapel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            importLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values"))
                        },
                        enabled = contentType != DeckContentType.SUBDECKS,
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "CSV importieren")
                    }
                    IconButton(onClick = { onSettings(deckId) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                    IconButton(
                        onClick = { onStudy(deckId) },
                        enabled = contentType == DeckContentType.CARDS || contentType == DeckContentType.SUBDECKS,
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Lernen")
                    }
                },
            )
        },
        floatingActionButton = {
            when (contentType) {
                DeckContentType.CARDS -> AddButton("Karte") { onAddCard(deckId) }
                DeckContentType.SUBDECKS -> AddButton("Unterstapel") { showAddSubDeck = true }
                DeckContentType.EMPTY -> AddButton("Hinzufügen") { showAddChoice = true }
                null -> Unit
            }
        },
    ) { padding ->
        when (contentType) {
            DeckContentType.CARDS -> {
                if (cards.isEmpty()) {
                    EmptyDeckContent(padding, "Noch keine Karten – füge eine Karte hinzu.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                    ) {
                        items(cards, key = { it.id }) { card ->
                            CardRow(card) { onEditCard(deckId, card.id) }
                        }
                    }
                }
            }
            DeckContentType.SUBDECKS -> {
                if (subDecks.isEmpty()) {
                    EmptyDeckContent(padding, "Noch keine Unterstapel – füge einen Unterstapel hinzu.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                    ) {
                        items(subDecks, key = { it.deck.id }) { subDeck ->
                            SubDeckRow(subDeck) { onOpenDeck(subDeck.deck.id) }
                        }
                    }
                }
            }
            DeckContentType.EMPTY -> EmptyDeckContent(
                padding,
                "Dieser Stapel ist leer. Füge entweder Karten oder Unterstapel hinzu.",
            )
            null -> Unit
        }
    }

    if (showAddChoice) {
        AlertDialog(
            onDismissRequest = { showAddChoice = false },
            title = { Text("Was möchtest du hinzufügen?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ein Stapel kann Karten oder Unterstapel enthalten, aber nicht beides.")
                    ExtendedFloatingActionButton(
                        text = { Text("Karte") },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = { showAddChoice = false; onAddCard(deckId) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ExtendedFloatingActionButton(
                        text = { Text("Unterstapel") },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        onClick = { showAddChoice = false; showAddSubDeck = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddChoice = false }) { Text("Abbrechen") } },
        )
    }

    if (showAddSubDeck) {
        NameDialog(
            title = "Neuer Unterstapel",
            onConfirm = { name ->
                vm.addSubDeck(name) { created ->
                    if (created) showAddSubDeck = false
                    else Toast.makeText(context, "Ein Unterstapel mit diesem Namen existiert bereits.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAddSubDeck = false },
        )
    }
}

@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        text = { Text(text) },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
private fun EmptyDeckContent(padding: androidx.compose.foundation.layout.PaddingValues, message: String) {
    Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubDeckRow(subDeck: DeckWithCounts, onClick: () -> Unit) {
    val contentType = subDeck.deck.contentType.toDeckContentType()
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "📁 ${subDeck.deck.name}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                when (contentType) {
                    DeckContentType.SUBDECKS -> "${subDeck.subDeckCount} Unterstapel"
                    DeckContentType.CARDS -> "${subDeck.totalCards} Karten"
                    DeckContentType.EMPTY -> "Leer"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun String.toDeckContentType(): DeckContentType =
    runCatching { DeckContentType.valueOf(this) }.getOrDefault(DeckContentType.EMPTY)

@Composable
private fun CardRow(card: CardEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            LatexText(card.front, fontSize = MaterialTheme.typography.titleMedium.fontSize)
            LatexText(
                card.back,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (card.tags.isNotBlank()) {
                Text(
                    "🏷 ${card.tags}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
