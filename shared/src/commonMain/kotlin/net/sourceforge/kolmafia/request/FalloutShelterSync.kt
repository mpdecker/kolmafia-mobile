package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop PlaceRequest falloutshelter whichplace sync (Phases 2361–2375).
 */
object FalloutShelterSync {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        preferences?.setBoolean("falloutShelterAvailable", true)
        if (html.contains("vault_term", ignoreCase = true) ||
            html.contains("Source Terminal", ignoreCase = true)
        ) {
            preferences?.setBoolean("falloutShelterTerminalAvailable", true)
        }
        val action = PlaceSync.action(url)
        when {
            action.contains("vault1") -> {
                preferences?.setBoolean("_falloutShelterVault1Used", true)
            }
            action.contains("vault3") || action.contains("spa") -> {
                preferences?.setBoolean(FalloutShelterRequest.SPA_USED_PREF, true)
            }
            action.contains("vault_term") || action.contains("terminal") -> {
                preferences?.setBoolean("falloutShelterTerminalAvailable", true)
            }
        }
        // Chroner / fuel / rads style lines
        Regex("""([\d,]+)\s*Chroner""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
            ?.let { preferences?.setInt("availableChroner", it) }
        ResultProcessor.processResults(false, html, null, character, preferences)
    }
}
