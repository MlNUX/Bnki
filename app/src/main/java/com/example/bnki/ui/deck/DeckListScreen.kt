package com.example.bnki.ui.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bnki.data.Deck

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    onOpenDeck: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onStudyAll: () -> Unit,
    vm: DeckListViewModel = viewModel(),
) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val totalDue = decks.sumOf { it.dueCards }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("bnki") },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistik")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Stapel hinzufügen")
            }
        },
    ) { padding ->
        if (decks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Noch keine Stapel – leg los! ➕", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Button(
                    onClick = onStudyAll,
                    enabled = totalDue > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text(
                        if (totalDue > 0) "  Lernen starten ($totalDue fällig)"
                        else "  Nichts fällig heute 🎉",
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
                ) {
                    items(decks, key = { it.deck.id }) { dwc ->
                        DeckRow(dwc.deck, dwc.totalCards, dwc.dueCards) { onOpenDeck(dwc.deck.id) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        NameDialog(
            title = "Neuer Stapel",
            onConfirm = { name -> vm.addDeck(name); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun DeckRow(deck: Deck, total: Int, due: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                deck.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val dueText = if (due > 0) "$due fällig heute" else "nichts fällig"
            Text(
                "$dueText · $total Karten",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
