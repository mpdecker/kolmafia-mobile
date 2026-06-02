package net.sourceforge.kolmafia.ui.familiar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamiliarDetailSheet(
    familiar: FamiliarData,
    familiarManager: FamiliarManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(familiar.name, style = MaterialTheme.typography.titleLarge)
            Text(familiar.race, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Weight: ${familiar.weight} lbs", style = MaterialTheme.typography.bodyMedium)
            Text("Experience: ${familiar.experience}", style = MaterialTheme.typography.bodyMedium)
            Text("Kills: ${familiar.kills}", style = MaterialTheme.typography.bodyMedium)
            familiar.equipment?.let {
                Text("Equipment: ${it.name}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = {
                    scope.launch {
                        familiarManager.switchFamiliar(familiar)
                        onDismiss()
                    }
                }) { Text("Make Active") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDismiss) { Text("Close") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
