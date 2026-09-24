package com.example.bnki.ui.deck

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bnki.data.Deck
import com.example.bnki.data.DeckContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    onOpenDeck: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onStudyAll: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: DeckListViewModel = viewModel(),
) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val devMode by vm.devMode.collectAsStateWithLifecycle()
    val totalDue by vm.totalDue.collectAsStateWithLifecycle()
    val totalCards by vm.totalCards.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("bnki", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistik")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Stapel")
            }
        },
    ) { padding ->
        if (decks.isEmpty()) {
            EmptyState(Modifier.fillMaxSize().padding(padding))
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                DashboardHeader(totalCards = totalCards, totalDue = totalDue)
                Button(
                    onClick = onStudyAll,
                    enabled = devMode && totalCards > 0 || totalDue > 0,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(60.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            devMode -> "Test-Abfrage · alle $totalCards Karten"
                            totalDue > 0 -> "Lernen starten · $totalDue fällig"
                            else -> "Nichts fällig heute 🎉"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    "Deine Stapel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(decks, key = { it.deck.id }) { dwc ->
                        DeckRow(
                            deck = dwc.deck,
                            total = dwc.totalCards,
                            due = dwc.dueCards,
                            subDeckCount = dwc.subDeckCount,
                        ) { onOpenDeck(dwc.deck.id) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        NameDialog(
            title = "Neuer Stapel",
            onConfirm = { name ->
                vm.addDeck(name) { created ->
                    if (created) showAdd = false
                    else Toast.makeText(context, "Ein Stapel mit diesem Namen existiert bereits.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun DashboardHeader(totalCards: Int, totalDue: Int) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
        Text("Bereit zum Lernen?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (totalDue > 0) "$totalDue Karten warten heute auf dich."
            else "$totalCards Karten in deinen Stapeln.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🗂️", style = MaterialTheme.typography.displaySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Noch keine Stapel", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tippe auf ➕, um deinen ersten Stapel anzulegen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeckRow(deck: Deck, total: Int, due: Int, subDeckCount: Int, onClick: () -> Unit) {
    val contentType = runCatching { DeckContentType.valueOf(deck.contentType) }
        .getOrDefault(DeckContentType.EMPTY)
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeckIcon(contentType, due)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    when (contentType) {
                        DeckContentType.SUBDECKS -> "$subDeckCount Unterstapel"
                        DeckContentType.CARDS -> "$total Karten"
                        DeckContentType.EMPTY -> "Leer"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (due > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "fällig",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

/** Icon mit Anzahl fälliger Karten für Kartenstapel bzw. Ordner-Symbol für Unterstapel. */
@Composable
private fun DeckIcon(contentType: DeckContentType, due: Int) {
    val hasDue = due > 0
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (hasDue) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (hasDue) {
                Text(
                    due.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Icon(
                    if (contentType == DeckContentType.SUBDECKS) Icons.Default.Folder else Icons.Default.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
