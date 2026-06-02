package net.sourceforge.kolmafia.ui.inventory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.inventory.CraftMode
import net.sourceforge.kolmafia.inventory.InventoryManager
import org.koin.compose.koinInject

@Composable
fun CraftingScreen() {
    val inventoryManager: InventoryManager = koinInject()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var item1Id by remember { mutableStateOf("") }
    var item2Id by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val modes = listOf(CraftMode.COMBINE, CraftMode.COOK, CraftMode.COCKTAIL, CraftMode.SMITH)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Crafting", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTab) {
            modes.forEachIndexed { i, mode ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = item1Id,
            onValueChange = { item1Id = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Item 1 ID") }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = item2Id,
            onValueChange = { item2Id = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Item 2 ID") }
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = {
                    val id1 = item1Id.toIntOrNull() ?: return@Button
                    val id2 = item2Id.toIntOrNull() ?: return@Button
                    scope.launch {
                        inventoryManager.craft(modes[selectedTab], id1, id2).fold(
                            onSuccess = { resultMessage = "Crafted: ${it.name}" },
                            onFailure = { resultMessage = "Failed: ${it.message}" }
                        )
                    }
                },
                enabled = item1Id.isNotBlank() && item2Id.isNotBlank()
            ) { Text("Craft") }
            Spacer(Modifier.width(8.dp))
            resultMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("Failed")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary)
            }
        }
    }
}
