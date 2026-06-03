package net.sourceforge.kolmafia.ui.mall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import net.sourceforge.kolmafia.mall.MallListing
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import org.koin.compose.koinInject

@Composable
fun MallScreen(
    searchRequest: MallSearchRequest = koinInject(),
    purchaseRequest: MallPurchaseRequest = koinInject()
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var listings by remember { mutableStateOf<List<MallListing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search the mall…") },
                singleLine = true
            )
            Button(onClick = {
                scope.launch {
                    isLoading = true
                    statusMessage = ""
                    listings = searchRequest.search(query.trim(), limit = 20)
                    isLoading = false
                }
            }) { Text("Search") }
        }

        if (statusMessage.isNotEmpty()) {
            Text(statusMessage, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(listings) { listing ->
                    MallListingCard(listing = listing, onBuy = {
                        scope.launch {
                            val result = purchaseRequest.buy(
                                listing.shopId, listing.itemId, 1, listing.price)
                            statusMessage = if (result.isSuccess) "Purchased!" else "Purchase failed"
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun MallListingCard(listing: MallListing, onBuy: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Item #${listing.itemId}", style = MaterialTheme.typography.bodyMedium)
                Text("Shop #${listing.shopId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${listing.price} Meat")
                Text("Qty: ${listing.quantity}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onBuy) { Text("Buy") }
            }
        }
    }
}
