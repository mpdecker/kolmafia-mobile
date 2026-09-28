package net.sourceforge.kolmafia.ui.relay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.webui.RelayLoader
import net.sourceforge.kolmafia.webui.RelayServer
import net.sourceforge.kolmafia.webui.UseLinkSpeculation
import org.koin.compose.koinInject

/**
 * In-app Relay Browser — starts the loopback server and loads game.php in a platform WebView.
 */
@Composable
fun RelayBrowserScreen() {
    val library: GameRuntimeLibrary = koinInject()
    val preferences: Preferences = koinInject()
    var url by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Starting Relay…") }

    DisposableEffect(Unit) {
        RelayServer.library = library
        RelayServer.preferences = preferences
        UseLinkSpeculation.library = library
        val opened = RelayLoader.openRelayBrowser(library, preferences)
        if (opened != null) {
            url = opened
            status = "Relay on port ${RelayServer.getPort()}"
            preferences.setBoolean("relayActive", true)
        } else {
            status = "Failed to start Relay server"
        }
        onDispose {
            // Keep server running across tab switches; stop via CLI `relay stop`.
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(status)
        Button(
            onClick = {
                RelayLoader.stopRelayServer()
                url = null
                status = "Relay stopped"
            },
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Text("Stop Relay")
        }
        Button(
            onClick = {
                val opened = RelayLoader.openRelayBrowser(library, preferences)
                url = opened
                status = if (opened != null) {
                    "Relay on port ${RelayServer.getPort()}"
                } else {
                    "Failed to start Relay server"
                }
            },
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Text("Restart Relay")
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val current = url
            if (current != null) {
                RelayBrowserView(url = current, modifier = Modifier.fillMaxSize())
            } else {
                Text("Relay is not running.")
            }
        }
    }
}

/** Platform WebView that loads the local Relay URL. */
@Composable
expect fun RelayBrowserView(url: String, modifier: Modifier = Modifier)
