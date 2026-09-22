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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bnki.data.Card as CardEntity
import com.example.bnki.ui.common.LatexText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId: Long,
    onBack: () -> Unit,
    onStudy: (Long) -> Unit,
    onAddCard: (Long) -> Unit,
    onEditCard: (Long, Long) -> Unit,
    onSettings: (Long) -> Unit,
    vm: DeckDetailViewModel = viewModel(),
) {
    LaunchedEffect(deckId) { vm.load(deckId) }
    val deck by vm.deck.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                    IconButton(onClick = { importLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values")) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "CSV importieren")
                    }
                    IconButton(onClick = { onSettings(deckId) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                    IconButton(onClick = { onStudy(deckId) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Lernen")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Karte") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { onAddCard(deckId) },
            )
        },
    ) { padding ->
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Noch keine Karten – füg welche hinzu ➕")
            }
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
}

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
