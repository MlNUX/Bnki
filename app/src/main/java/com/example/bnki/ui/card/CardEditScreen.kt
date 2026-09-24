package com.example.bnki.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TopAppBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bnki.ui.common.LatexText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditScreen(
    deckId: Long,
    cardId: Long,
    onBack: () -> Unit,
    vm: CardEditViewModel = viewModel(),
) {
    LaunchedEffect(deckId, cardId) { vm.load(deckId, cardId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showPreview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (cardId == 0L) "Neue Karte" else "Karte bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.save(onBack) },
                        enabled = state.front.isNotBlank() && state.back.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Speichern")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !showPreview,
                    onClick = { showPreview = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Bearbeiten") }
                SegmentedButton(
                    selected = showPreview,
                    onClick = { showPreview = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Vorschau") }
            }

            if (showPreview) {
                CardPreview(
                    front = state.front,
                    back = state.back,
                    hint = state.hint,
                    tags = state.tags,
                )
            } else {
                Text(
                    "Tipp: Formeln in \$...\$ (z. B. \$x^2 + 1\$), Code in ```...``` schreiben.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.front,
                    onValueChange = vm::onFront,
                    label = { Text("Vorderseite (Frage)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                OutlinedTextField(
                    value = state.back,
                    onValueChange = vm::onBack,
                    label = { Text("Rückseite (Antwort)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                OutlinedTextField(
                    value = state.hint,
                    onValueChange = vm::onHint,
                    label = { Text("Hinweis / Tipp (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = state.tags,
                    onValueChange = vm::onTags,
                    label = { Text("Tags (kommagetrennt)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CardPreview(
    front: String,
    back: String,
    hint: String,
    tags: String,
) {
    if (front.isBlank() && back.isBlank() && hint.isBlank() && tags.isBlank()) {
        Text(
            "Noch nichts eingegeben – wechsle zu 'Bearbeiten'.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreviewSection("Vorderseite", front)
            if (back.isNotBlank()) {
                HorizontalDivider()
                PreviewSection("Rückseite", back)
            }
            if (hint.isNotBlank()) {
                HorizontalDivider()
                PreviewSection("Hinweis", hint)
            }
            if (tags.isNotBlank()) {
                HorizontalDivider()
                Text(
                    "Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(tags, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PreviewSection(label: String, content: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (content.isNotBlank()) {
        LatexText(content)
    } else {
        Text(
            "(leer)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
