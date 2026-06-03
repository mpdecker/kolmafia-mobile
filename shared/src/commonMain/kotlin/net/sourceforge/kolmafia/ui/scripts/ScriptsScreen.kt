package net.sourceforge.kolmafia.ui.scripts

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.ash.ScriptEntry
import net.sourceforge.kolmafia.ash.ScriptManager
import net.sourceforge.kolmafia.ash.currentTimeMillis
import org.koin.compose.koinInject

@Composable
fun ScriptsScreen(
    onEditScript: (ScriptEntry?) -> Unit,
    onShowConsole: (String) -> Unit
) {
    val scriptManager: ScriptManager = koinInject()
    val state by scriptManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Scripts", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { onEditScript(null) }) { Text("New Script") }
        }

        if (state.scripts.isEmpty()) {
            Text(
                "No scripts yet. Tap 'New Script' to create one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.scripts, key = { it.name }) { script ->
                    ScriptRow(
                        script = script,
                        isRunning = state.runningScript == script.name,
                        onRun = {
                            scriptManager.runScript(script.name, scope)
                            onShowConsole(script.name)
                        },
                        onEdit = { onEditScript(script) },
                        onDelete = { scriptManager.deleteScript(script.name) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ScriptRow(
    script: ScriptEntry,
    isRunning: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(script.name, style = MaterialTheme.typography.bodyMedium)
            if (script.lastRunAt > 0L) {
                Text(
                    "Last run: ${formatRelativeTime(script.lastRunAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (confirmDelete) {
            TextButton(
                onClick = { onDelete(); confirmDelete = false },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Confirm") }
            TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
        } else {
            Button(onClick = onRun, enabled = !isRunning) {
                Text(if (isRunning) "Running…" else "Run")
            }
            Spacer(Modifier.width(4.dp))
            OutlinedButton(onClick = onEdit) { Text("Edit") }
            Spacer(Modifier.width(4.dp))
            TextButton(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Delete") }
        }
    }
}

/** Simple relative-time formatting without external dependencies. */
private fun formatRelativeTime(epochMillis: Long): String {
    val diffSec = (currentTimeMillis() - epochMillis) / 1000
    return when {
        diffSec < 60      -> "just now"
        diffSec < 3600    -> "${diffSec / 60}m ago"
        diffSec < 86400   -> "${diffSec / 3600}h ago"
        else              -> "${diffSec / 86400}d ago"
    }
}
