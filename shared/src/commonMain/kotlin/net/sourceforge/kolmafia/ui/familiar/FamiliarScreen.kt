package net.sourceforge.kolmafia.ui.familiar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import org.koin.compose.koinInject

@Composable
fun FamiliarScreen() {
    val familiarManager: FamiliarManager = koinInject()
    val state by familiarManager.state.collectAsState()
    var selectedFamiliar by remember { mutableStateOf<FamiliarData?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Familiars", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        state.activeFamiliar?.let { active ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active: ${active.name}", style = MaterialTheme.typography.titleMedium)
                        Text("${active.weight} lbs", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(active.race, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Kills: ${active.kills} · Exp: ${active.experience}",
                        style = MaterialTheme.typography.bodySmall)
                    active.equipment?.let {
                        Text("Equipped: ${it.name}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } ?: Text("No familiar active", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text("Terrarium (${state.ownedFamiliars.size})", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.ownedFamiliars, key = { it.id }) { familiar ->
                FamiliarGridCard(familiar, onClick = { selectedFamiliar = familiar })
            }
        }
    }

    selectedFamiliar?.let { familiar ->
        FamiliarDetailSheet(
            familiar = familiar,
            familiarManager = familiarManager,
            onDismiss = { selectedFamiliar = null }
        )
    }
}

@Composable
private fun FamiliarGridCard(familiar: FamiliarData, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(familiar.name, style = MaterialTheme.typography.bodyMedium)
            Text(familiar.race, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${familiar.weight} lbs", style = MaterialTheme.typography.bodySmall)
        }
    }
}
