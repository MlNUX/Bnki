package com.example.bnki.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bnki.data.AnswerInputMode
import com.example.bnki.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    val devMode by vm.devMode.collectAsStateWithLifecycle()
    val canvasGrid by vm.canvasGrid.collectAsStateWithLifecycle()
    val answerInputMode by vm.answerInputMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Erscheinungsbild",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            ThemeOption("System", "Folgt den Systemeinstellungen", mode == ThemeMode.SYSTEM) {
                vm.setThemeMode(ThemeMode.SYSTEM)
            }
            ThemeOption("Hell", "Immer helles Design", mode == ThemeMode.LIGHT) {
                vm.setThemeMode(ThemeMode.LIGHT)
            }
            ThemeOption("Dunkel", "Immer dunkles Design", mode == ThemeMode.DARK) {
                vm.setThemeMode(ThemeMode.DARK)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                "Antwort eingeben",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            ThemeOption(
                "Mit Stift schreiben",
                "Antworten auf der Zeichenfläche handschriftlich eingeben.",
                answerInputMode == AnswerInputMode.STYLUS,
            ) { vm.setAnswerInputMode(AnswerInputMode.STYLUS) }
            ThemeOption(
                "Eintippen",
                "Antworten als Text über die Tastatur eingeben.",
                answerInputMode == AnswerInputMode.TYPING,
            ) { vm.setAnswerInputMode(AnswerInputMode.TYPING) }
            ThemeOption(
                "Beim Lernen wechseln",
                "In der Lernansicht direkt zwischen Stift und Tastatur wechseln.",
                answerInputMode == AnswerInputMode.SWITCH,
            ) { vm.setAnswerInputMode(AnswerInputMode.SWITCH) }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                "Zeichenfläche",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            SwitchRow(
                title = "Kariert",
                subtitle = "Rasterlinien auf der Schreibfläche anzeigen.",
                checked = canvasGrid,
                onCheckedChange = { vm.setCanvasGrid(it) },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(
                "Entwickler",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            SwitchRow(
                title = "Dev-Modus",
                subtitle = "Immer alle Karten abfragen – ignoriert Fälligkeit und Tageslimit.",
                checked = devMode,
                onCheckedChange = { vm.setDevMode(it) },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
