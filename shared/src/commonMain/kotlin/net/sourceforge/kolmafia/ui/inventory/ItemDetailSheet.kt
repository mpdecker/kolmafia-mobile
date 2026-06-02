package net.sourceforge.kolmafia.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    item: InventoryItem,
    inventoryManager: InventoryManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name.ifBlank { "Item #${item.itemId}" },
                style = MaterialTheme.typography.titleLarge)
            Text("Quantity: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
            Text("Type: ${item.type.name.lowercase().replace('_', ' ')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.type in listOf(ItemType.FOOD, ItemType.DRINK, ItemType.SPLEEN,
                        ItemType.USABLE, ItemType.MULTIUSABLE, ItemType.REUSABLE)) {
                    Button(onClick = {
                        scope.launch { inventoryManager.useItem(item); onDismiss() }
                    }) { Text("Use") }
                }
                if (item.type in listOf(ItemType.WEAPON, ItemType.OFFHAND, ItemType.HAT,
                        ItemType.SHIRT, ItemType.PANTS, ItemType.ACCESSORY)) {
                    Button(onClick = {
                        scope.launch {
                            inventoryManager.equipItem(item, item.type.name.lowercase())
                            onDismiss()
                        }
                    }) { Text("Equip") }
                }
                OutlinedButton(onClick = {
                    scope.launch { inventoryManager.discardItem(item, 1); onDismiss() }
                }) { Text("Discard") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
