package com.example.bnki.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bnki.domain.Sm2
import com.example.bnki.ui.common.HandwritingCanvas
import com.example.bnki.ui.common.LatexText
import com.example.bnki.ui.common.rememberHandwritingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    deckId: Long,
    onBack: () -> Unit,
    vm: StudyViewModel = viewModel(),
) {
    LaunchedEffect(deckId) { vm.load(deckId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val handwriting = rememberHandwritingState()
    var showHint by remember { mutableStateOf(false) }
    val canvasGrid by vm.canvasGrid.collectAsStateWithLifecycle()

    // Bei Kartenwechsel: Zeichenfläche und Hinweis zurücksetzen.
    LaunchedEffect(state.index) {
        handwriting.clear()
        handwriting.resetView()
        showHint = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lernen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    val hasHint = state.current?.hint?.isNotBlank() == true
                    IconButton(
                        onClick = { showHint = !showHint },
                        enabled = hasHint,
                    ) {
                        Icon(
                            Icons.Outlined.Lightbulb,
                            contentDescription = "Tipp anzeigen",
                            tint = if (showHint && hasHint) MaterialTheme.colorScheme.tertiary
                            else LocalContentColor.current,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                state.finished -> {
                    FinishedView(state.reviewedCount, onBack)
                }
                else -> {
                    val card = state.current!!
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        // Frage
                        LatexText(card.front, fontSize = MaterialTheme.typography.headlineSmall.fontSize)

                        if (card.hint.isNotBlank() && showHint) {
                            LatexText(
                                "💡 ${card.hint}",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            )
                        }

                        HorizontalDivider(Modifier.padding(vertical = 8.dp))

                        if (!state.revealed) {
                            Text(
                                "Antwort mit dem Stift schreiben:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            HandwritingCanvas(
                                state = handwriting,
                                grid = canvasGrid,
                                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { handwriting.undo() }) { Text("Rückgängig") }
                                OutlinedButton(onClick = { handwriting.clear() }) { Text("Löschen") }
                            }
                            Button(
                                onClick = { vm.reveal(wasBlank = handwriting.isEmpty) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) { Text("Auflösen") }
                        } else {
                            // Auflösung: richtige Antwort + Bewertung
                            Text("Richtige Antwort:", style = MaterialTheme.typography.labelMedium)
                            LatexText(card.back, fontSize = MaterialTheme.typography.titleLarge.fontSize)

                            Box(Modifier.weight(1f))

                            if (state.autoWrong) {
                                Text(
                                    "Nichts geschrieben → als falsch gewertet.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Button(
                                    onClick = { vm.grade(Sm2.QUALITY_BLANK) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                ) { Text("Weiter") }
                            } else {
                                Text(
                                    "Wie gut wusstest du es?",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = { vm.grade(Sm2.QUALITY_AGAIN) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                        ),
                                    ) { Text("Nochmal") }
                                    FilledTonalButton(
                                        onClick = { vm.grade(Sm2.QUALITY_GOOD) },
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Gut") }
                                    Button(
                                        onClick = { vm.grade(Sm2.QUALITY_EASY) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2E7D32),
                                        ),
                                    ) { Text("Leicht") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishedView(count: Int, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Für heute geschafft ✅", style = MaterialTheme.typography.headlineSmall)
        Text(
            "$count Karten gelernt 🎉",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("Fertig") }
    }
}
