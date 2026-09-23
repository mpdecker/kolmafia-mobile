package net.sourceforge.kolmafia.webui

import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [RelayLoader] — start RelayServer and expose the default browser URL.
 */
object RelayLoader {
    fun startRelayServer(
        library: GameRuntimeLibrary? = null,
        preferences: Preferences? = null,
    ): Boolean {
        if (library != null) {
            RelayServer.library = library
            UseLinkSpeculation.library = library
        }
        if (preferences != null) RelayServer.preferences = preferences
        return RelayServer.startThread()
    }

    /** Desktop opens `http://127.0.0.1:<port>/game.php` (main.php rewritten). */
    fun relayBrowserUrl(): String {
        val port = RelayServer.getPort()
        return "http://127.0.0.1:$port/game.php"
    }

    fun openRelayBrowser(
        library: GameRuntimeLibrary? = null,
        preferences: Preferences? = null,
    ): String? {
        if (!startRelayServer(library, preferences)) return null
        // Wait briefly for listen
        var attempts = 0
        while (!RelayServer.isRunning() && attempts < 50) {
            attempts++
            // busy-wait tiny; platform start is usually sync for bind
        }
        return if (RelayServer.isRunning()) relayBrowserUrl() else null
    }

    fun stopRelayServer() {
        RelayServer.stop()
    }
}
