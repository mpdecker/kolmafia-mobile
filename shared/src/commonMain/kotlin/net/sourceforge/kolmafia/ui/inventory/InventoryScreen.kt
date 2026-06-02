package net.sourceforge.kolmafia.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import org.koin.compose.koinInject

private val TABS = listOf("All", "Equipment", "Food", "Drink", "Usable", "Other")

@Composable
fun InventoryScreen() {
    val inventoryManager: InventoryManager = koinInject()
    val state by inventoryManager.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }

    val filteredItems = remember(selectedTab, state.items) {
        state.items.values.filter { item ->
            when (selectedTab) {
                0 -> true
                1 -> item.type in listOf(ItemType.WEAPON, ItemType.OFFHAND, ItemType.HAT,
                    ItemType.SHIRT, ItemType.PANTS, ItemType.ACCESSORY)
                2 -> item.type == ItemType.FOOD
                3 -> item.type == ItemType.DRINK
                4 -> item.type in listOf(ItemType.USABLE, ItemType.MULTIUSABLE, ItemType.REUSABLE)
                else -> item.type in listOf(ItemType.OTHER, ItemType.SPLEEN, ItemType.FAMILIAR_ITEM)
            }
        }.sortedBy { it.name }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Inventory", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp))
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            TABS.forEachIndexed { i, label ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                    Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredItems, key = { it.itemId }) { item ->
                InventoryItemRow(item, onClick = { selectedItem = item })
                HorizontalDivider()
            }
        }
    }

    selectedItem?.let { item ->
        ItemDetailSheet(
            item = item,
            inventoryManager = inventoryManager,
            onDismiss = { selectedItem = null }
        )
    }
}

@Composable
private fun InventoryItemRow(item: InventoryItem, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)) {
        Text(item.name.ifBlank { "Item #${item.itemId}" },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text("×${item.quantity}", style = MaterialTheme.typography.bodyMedium)
    }
}
