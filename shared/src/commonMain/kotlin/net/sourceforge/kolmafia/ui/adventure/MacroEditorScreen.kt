package net.sourceforge.kolmafia.ui.adventure

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.adventure.MacroStrategy
import net.sourceforge.kolmafia.preferences.Preferences
import org.koin.compose.koinInject

@Composable
fun MacroEditorScreen() {
    val preferences: Preferences = koinInject()
    var selectedTab by remember { mutableIntStateOf(0) }
    var globalMacro by remember {
        mutableStateOf(preferences.getString("combatMacroDefault", MacroStrategy.SAFE_DEFAULT))
    }
    var zoneId by remember { mutableStateOf("") }
    var zoneMacro by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Combat Macros", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Global Default") }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Per-Zone") }
        }
        Spacer(Modifier.height(12.dp))
        when (selectedTab) {
            0 -> {
                Text("Applied to all zones unless overridden:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = globalMacro,
                    onValueChange = { globalMacro = it; saved = false },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    label = { Text("Macro") }
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    preferences.setString("combatMacroDefault", globalMacro)
                    saved = true
                }) { Text("Save") }
            }
            1 -> {
                OutlinedTextField(
                    value = zoneId,
                    onValueChange = {
                        zoneId = it
                        zoneMacro = preferences.getString("combatMacro_$it")
                        saved = false
                    },
                    label = { Text("Zone ID (snarfblat)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = zoneMacro,
                    onValueChange = { zoneMacro = it; saved = false },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    label = { Text("Zone macro (blank = use global)") }
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (zoneId.isNotBlank()) preferences.setString("combatMacro_$zoneId", zoneMacro)
                        saved = true
                    },
                    enabled = zoneId.isNotBlank()
                ) { Text("Save") }
            }
        }
        if (saved) {
            Spacer(Modifier.height(4.dp))
            Text("Saved", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
