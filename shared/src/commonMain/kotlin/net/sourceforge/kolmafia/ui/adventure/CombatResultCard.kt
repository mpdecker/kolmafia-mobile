package net.sourceforge.kolmafia.ui.adventure

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.adventure.AdventureResult

@Composable
fun CombatResultCard(result: AdventureResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (result) {
                is AdventureResult.Combat -> if (result.won)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
                is AdventureResult.NonCombat -> MaterialTheme.colorScheme.secondaryContainer
                is AdventureResult.Choice -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            when (result) {
                is AdventureResult.Combat -> {
                    Text(
                        if (result.won) "Win: ${result.monster}" else "Loss: ${result.monster}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (result.meatGained > 0)
                        Text("Meat: +${result.meatGained}", style = MaterialTheme.typography.bodySmall)
                    result.itemsGained.forEach { item ->
                        Text("Item: $item", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AdventureResult.NonCombat -> {
                    Text(result.encounterName, style = MaterialTheme.typography.titleSmall)
                    if (result.meatGained > 0)
                        Text("Meat: +${result.meatGained}", style = MaterialTheme.typography.bodySmall)
                    result.itemsGained.forEach { item ->
                        Text("Item: $item", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AdventureResult.Choice -> {
                    Text("Choice: ${result.encounterName}", style = MaterialTheme.typography.titleSmall)
                    val chosenText = result.chosenOption?.let { "Chose option $it" } ?: "Choice pending"
                    Text(chosenText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
