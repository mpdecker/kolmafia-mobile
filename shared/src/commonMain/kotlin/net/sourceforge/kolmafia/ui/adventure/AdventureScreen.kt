package net.sourceforge.kolmafia.ui.adventure

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureResult
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import org.koin.compose.koinInject

@Composable
fun AdventureScreen() {
    val adventureManager: AdventureManager = koinInject()
    val eventBus: GameEventBus = koinInject()
    val isRunning by adventureManager.isRunning.collectAsState()
    val scope = rememberCoroutineScope()
    val results = remember { mutableStateListOf<AdventureResult>() }
    var zoneId by remember { mutableStateOf("") }
    var zoneName by remember { mutableStateOf("") }
    var turnsText by remember { mutableStateOf("10") }
    var stopMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        eventBus.events.collect { event ->
            when (event) {
                is GameEvent.TurnConsumed -> results.add(0, event.result)
                is GameEvent.AdventureLoopStopped -> stopMessage = "Stopped: ${event.reason}"
                else -> {}
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Adventure", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = zoneId,
            onValueChange = { zoneId = it },
            label = { Text("Zone ID (snarfblat)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = zoneName,
            onValueChange = { zoneName = it },
            label = { Text("Zone name (display only)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = turnsText,
            onValueChange = { turnsText = it },
            label = { Text("Turns") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = {
                    stopMessage = null
                    val turns = turnsText.toIntOrNull() ?: 1
                    val location = AdventureLocation(zoneId, zoneName.ifBlank { "Zone $zoneId" }, "")
                    adventureManager.runAdventures(location, turns, scope)
                },
                enabled = !isRunning && zoneId.isNotBlank()
            ) { Text("Run $turnsText Turns") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { adventureManager.stop() },
                enabled = isRunning
            ) { Text("Stop") }
        }
        stopMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        Text("Results (${results.size} turns)", style = MaterialTheme.typography.labelLarge)
        LazyColumn {
            items(results) { result ->
                CombatResultCard(result, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
