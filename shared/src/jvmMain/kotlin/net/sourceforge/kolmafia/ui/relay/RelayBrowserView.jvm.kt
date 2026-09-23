package net.sourceforge.kolmafia.ui.relay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** JVM stub — no WebView in unit-test / desktop JVM target. */
@Composable
actual fun RelayBrowserView(url: String, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Relay WebView stub\n$url")
    }
}
