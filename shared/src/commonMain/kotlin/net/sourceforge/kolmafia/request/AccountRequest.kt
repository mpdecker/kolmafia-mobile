package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.AccountRequest]. */
object AccountRequest {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter?,
    ) {
        AccountSync.parseAccountData(url, html, preferences, character)
    }

    fun registerRequest(url: String): Boolean = url.contains("account.php", ignoreCase = true)
}
