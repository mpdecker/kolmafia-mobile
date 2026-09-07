package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharpaneStatusSync
import net.sourceforge.kolmafia.character.KoLCharacter

/** Desktop [net.sourceforge.kolmafia.request.CharPaneRequest] charpane.php hub. */
object CharPaneRequest {
    fun parseResponse(url: String, html: String, character: KoLCharacter?) {
        if (!url.contains("charpane.php", ignoreCase = true)) return
        val kol = character ?: return
        kol.updateFromCharpane(CharpaneStatusSync.parse(html, kol.state.value))
    }

    fun registerRequest(url: String): Boolean = url.contains("charpane.php", ignoreCase = true)
}
