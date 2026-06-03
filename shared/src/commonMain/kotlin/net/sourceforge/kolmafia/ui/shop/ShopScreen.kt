package net.sourceforge.kolmafia.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterRegistry
import net.sourceforge.kolmafia.shop.CoinmasterRequest
import net.sourceforge.kolmafia.shop.ShopRow
import org.koin.compose.koinInject

@Composable
fun ShopScreen(coinmasterRequest: CoinmasterRequest = koinInject()) {
    val scope = rememberCoroutineScope()
    var selectedMaster by remember { mutableStateOf<CoinmasterData?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    Row(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.width(160.dp).fillMaxHeight().padding(4.dp)) {
            items(CoinmasterRegistry.all) { master ->
                Text(
                    text = master.masterName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (master == selectedMaster) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMaster = master; statusMessage = "" }
                        .padding(8.dp)
                )
            }
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))

        Column(Modifier.fillMaxSize().padding(8.dp)) {
            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp))
            }

            val master = selectedMaster
            if (master == null) {
                Text("Select a coinmaster", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(master.masterName, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn {
                    items(master.buyItems) { row ->
                        ShopRowCard(row = row, onBuy = {
                            scope.launch {
                                val result = coinmasterRequest.buy(master, row.rowId, 1)
                                statusMessage = if (result.isSuccess) "Purchased!" else "Purchase failed"
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopRowCard(row: ShopRow, onBuy: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Item #${row.item.itemId}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = row.costs.joinToString { c ->
                    if (c.isMeat) "${c.count} Meat" else "${c.count}x #${c.itemId}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onBuy) { Text("Buy") }
    }
}
