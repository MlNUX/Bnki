package com.example.bnki.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import com.example.bnki.data.AnswerInputMode
import com.example.bnki.domain.Sm2
import com.example.bnki.ui.common.HandwritingCanvas
import com.example.bnki.ui.common.HandwritingState
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
    var showOwnAnswer by remember { mutableStateOf(false) }
    var typedAnswer by remember { mutableStateOf("") }
    var activeInputMode by remember { mutableStateOf(AnswerInputMode.STYLUS) }
    val canvasGrid by vm.canvasGrid.collectAsStateWithLifecycle()
    val answerInputMode by vm.answerInputMode.collectAsStateWithLifecycle()

    // Bei fest gewählter Eingabeart folgt die Lernansicht der Einstellung.
    // Im Umschaltmodus bleibt die zuletzt gewählte Eingabeart aktiv.
    LaunchedEffect(answerInputMode) {
        if (answerInputMode != AnswerInputMode.SWITCH) activeInputMode = answerInputMode
    }

    // Bei Kartenwechsel: Zeichenfläche und Hinweis zurücksetzen.
    LaunchedEffect(state.index) {
        handwriting.clear()
        handwriting.resetView()
        typedAnswer = ""
        showHint = false
        showOwnAnswer = false
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
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    "FRAGE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                LatexText(
                                    card.front,
                                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }

                        if (card.hint.isNotBlank() && showHint) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                Text(
                                    "💡 Hinweis",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                LatexText(
                                    card.hint,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                }
                            }
                        }

                        if (!state.revealed) {
                            if (answerInputMode == AnswerInputMode.SWITCH) {
                                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = activeInputMode == AnswerInputMode.STYLUS,
                                        onClick = { activeInputMode = AnswerInputMode.STYLUS },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    ) { Text("Stift") }
                                    SegmentedButton(
                                        selected = activeInputMode == AnswerInputMode.TYPING,
                                        onClick = { activeInputMode = AnswerInputMode.TYPING },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    ) { Text("Tastatur") }
                                }
                            }

                            when (activeInputMode) {
                                AnswerInputMode.STYLUS -> StylusAnswerInput(handwriting, canvasGrid)
                                AnswerInputMode.TYPING -> TypedAnswerInput(
                                    answer = typedAnswer,
                                    onAnswerChange = { typedAnswer = it },
                                )
                                AnswerInputMode.SWITCH -> Unit // Nur eine Einstellungsoption, nie aktiv.
                            }

                            Button(
                                onClick = {
                                    vm.reveal(wasBlank = handwriting.isEmpty && typedAnswer.isBlank())
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) { Text("Auflösen") }
                        } else {
                            // Auflösung: richtige Antwort + Bewertung
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "RICHTIGE ANTWORT",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    LatexText(
                                        card.back,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }

                            val hasOwnAnswer = typedAnswer.isNotBlank() || !handwriting.isEmpty
                            if (hasOwnAnswer) {
                                OutlinedButton(
                                    onClick = { showOwnAnswer = !showOwnAnswer },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        if (showOwnAnswer) "Eigene Antwort ausblenden"
                                        else "Eigene Antwort ansehen",
                                    )
                                }
                                if (showOwnAnswer) {
                                    OwnAnswerPanel(
                                        typedAnswer = typedAnswer,
                                        handwriting = handwriting,
                                        canvasGrid = canvasGrid,
                                    )
                                }
                            }

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
                                    ) { Text("Sicher") }
                                    Button(
                                        onClick = { vm.grade(Sm2.QUALITY_EASY) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2E7D32),
                                        ),
                                    ) { Text("Sehr sicher") }
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
private fun OwnAnswerPanel(
    typedAnswer: String,
    handwriting: HandwritingState,
    canvasGrid: Boolean,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "DEINE ANTWORT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (typedAnswer.isNotBlank()) {
                Text(typedAnswer, style = MaterialTheme.typography.bodyLarge)
            }
            if (!handwriting.isEmpty) {
                HandwritingCanvas(
                    state = handwriting,
                    grid = canvasGrid,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.StylusAnswerInput(handwriting: HandwritingState, canvasGrid: Boolean) {
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
}

@Composable
private fun ColumnScope.TypedAnswerInput(answer: String, onAnswerChange: (String) -> Unit) {
    Text(
        "Antwort eintippen:",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChange,
        label = { Text("Deine Antwort") },
        modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
        minLines = 5,
    )
}

@Composable
private fun FinishedView(count: Int, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(
                Modifier.padding(horizontal = 32.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🎉", style = MaterialTheme.typography.displayMedium)
                Text(
                    "Für heute geschafft",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "$count Karten gelernt",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("Fertig") }
            }
        }
    }
}
