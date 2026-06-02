package net.sourceforge.kolmafia.ui.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.ui.effects.EffectsScreen
import org.koin.compose.koinInject

@Composable
fun CharacterScreen(character: KoLCharacter) {
    val state by character.state.collectAsState()
    val effectManager: EffectManager = koinInject()
    val effectState by effectManager.state.collectAsState()
    var showEffects by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.name, style = MaterialTheme.typography.headlineMedium)
            if (state.isHardcore) {
                Text("HC", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error)
            }
        }
        Text("Level ${state.level}", style = MaterialTheme.typography.titleMedium)
        if (state.challengePath.isNotBlank() && state.challengePath != "None") {
            Text(state.challengePath, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.zodiacSign.isNotBlank()) {
            Text("Sign: ${state.zodiacSign}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.roninLeft > 0) {
            Text("Ronin: ${state.roninLeft} turns remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary)
        }

        Spacer(Modifier.height(16.dp))
        // HP/MP bars
        StatBar(label = "HP", current = state.currentHp, max = state.maxHp)
        Spacer(Modifier.height(8.dp))
        StatBar(label = "MP", current = state.currentMp, max = state.maxMp)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Combat stats
        Text("Stats", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        StatRow("Muscle",       state.baseMusc, state.muscSubpoints)
        StatRow("Mysticality",  state.baseMyst, state.mystSubpoints)
        StatRow("Moxie",        state.baseMoxie, state.moxieSubpoints)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Resources
        Text("Resources", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        InfoRow("Meat",        state.meat.toString())
        InfoRow("Adventures",  state.adventuresLeft.toString())
        InfoRow("Fullness",    "${state.fullness} / 15")
        InfoRow("Inebriety",   "${state.inebriety} / 14")
        InfoRow("Spleen",      "${state.spleenUsed} / 15")

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Effects (tap to open full list)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEffects = true }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Active Effects", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${effectState.effects.size} →",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        if (effectState.effects.isNotEmpty()) {
            effectState.effects.take(3).forEach { effect ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(effect.name, style = MaterialTheme.typography.bodySmall)
                    Text("${effect.duration} turns", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (effectState.effects.size > 3) {
                Text("… and ${effectState.effects.size - 3} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showEffects) {
        EffectsScreen(effectManager = effectManager, onDismiss = { showEffects = false })
    }
}

@Composable
private fun StatBar(label: String, current: Int, max: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$current / $max", style = MaterialTheme.typography.labelLarge)
        }
        if (max > 0) {
            LinearProgressIndicator(
                progress = { current.toFloat() / max.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatRow(label: String, base: Int, subpoints: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("$base ($subpoints xp)", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
