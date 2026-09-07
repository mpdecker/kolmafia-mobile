package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

/** Desktop [net.sourceforge.kolmafia.request.LogoutRequest]. */
object LogoutRequest {
    var lastResponse: String = ""
        private set
    var isRunning: Boolean = false
        private set

    fun parseResponse(url: String, html: String, character: KoLCharacter?) {
        if (!url.contains("logout.php", ignoreCase = true)) return
        lastResponse = html
        ChoiceCombatAshState.reset()
        character?.reset()
    }

    fun registerRequest(url: String): Boolean = url.contains("logout.php", ignoreCase = true)
}
