package net.sourceforge.kolmafia.ui.scripts

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.ash.ScriptEntry
import net.sourceforge.kolmafia.ash.ScriptManager
import org.koin.compose.koinInject

@Composable
fun ScriptEditorScreen(
    existingScript: ScriptEntry?,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val scriptManager: ScriptManager = koinInject()
    var name by remember { mutableStateOf(existingScript?.name ?: "") }
    var source by remember { mutableStateOf(existingScript?.source ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            if (existingScript == null) "New Script" else "Edit Script",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = null },
            label = { Text("Script name") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            // Can't rename an existing script (would need delete + re-add)
            enabled = existingScript == null
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = source,
            onValueChange = { source = it },
            label = { Text("Script source (ASH)") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            colors = TextFieldDefaults.colors()
        )
        Spacer(Modifier.height(8.dp))

        Row {
            Button(onClick = {
                if (name.isBlank()) { nameError = "Name is required"; return@Button }
                scriptManager.saveScript(
                    ScriptEntry(
                        name = name.trim(),
                        source = source,
                        lastRunAt = existingScript?.lastRunAt ?: 0L
                    )
                )
                onSaved()
            }) { Text("Save") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
