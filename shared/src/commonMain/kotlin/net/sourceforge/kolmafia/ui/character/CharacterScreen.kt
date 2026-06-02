package net.sourceforge.kolmafia.ui.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.character.KoLCharacter

@Composable
fun CharacterScreen(character: KoLCharacter) {
    val state by character.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(state.name, style = MaterialTheme.typography.headlineMedium)
        Text("Level ${state.level}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        StatBar(label = "HP", current = state.currentHp, max = state.maxHp)
        Spacer(Modifier.height(8.dp))
        StatBar(label = "MP", current = state.currentMp, max = state.maxMp)
        Spacer(Modifier.height(16.dp))
        InfoRow("Meat", state.meat.toString())
        InfoRow("Adventures", state.adventuresLeft.toString())
        InfoRow("Fullness", "${state.fullness} / 15")
        InfoRow("Inebriety", "${state.inebriety} / 14")
        InfoRow("Spleen", "${state.spleenUsed} / 15")
    }
}

@Composable
private fun StatBar(label: String, current: Int, max: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
