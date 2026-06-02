package net.sourceforge.kolmafia.ui.familiar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import org.koin.compose.koinInject

@Composable
fun HatcheryScreen() {
    val inventoryManager: InventoryManager = koinInject()
    val familiarManager: FamiliarManager = koinInject()
    val invState by inventoryManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    val eggs = invState.items.values.filter { it.type == ItemType.FAMILIAR_ITEM }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hatchery", style = MaterialTheme.typography.headlineSmall)
        Text("${eggs.size} hatchable eggs in inventory",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyColumn {
            items(eggs, key = { it.itemId }) { egg ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(egg.name, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        scope.launch { familiarManager.hatch(egg.itemId) }
                    }) { Text("Hatch") }
                }
                HorizontalDivider()
            }
        }
    }
}
