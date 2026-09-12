package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop PlaceRequest falloutshelter whichplace sync (Phases 2361–2375).
 * Deepened in Behavioral Deepen XLI (6311–6330) toward desktop FalloutShelterRequest.parseResponse.
 */
object FalloutShelterSync {
    private val SHELTER_LEVEL = Regex(
        """vault(?:level)?(\d+)""",
        RegexOption.IGNORE_CASE,
    )

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        val prefs = preferences
        prefs?.setBoolean("falloutShelterAvailable", true)

        // Desktop iterates all vaultN.gif hits and keeps the highest level.
        var shelterLevel = -1
        Regex("""vault(\d+)\.gif""", RegexOption.IGNORE_CASE).findAll(html).forEach { m ->
            m.groupValues.getOrNull(1)?.toIntOrNull()?.let { level ->
                if (level > shelterLevel) shelterLevel = level
            }
        }
        if (shelterLevel < 0) {
            SHELTER_LEVEL.findAll(html).forEach { m ->
                m.groupValues.getOrNull(1)?.toIntOrNull()?.let { level ->
                    if (level > shelterLevel) shelterLevel = level
                }
            }
        }
        if (shelterLevel > 0) {
            prefs?.setInt("falloutShelterLevel", shelterLevel)
        }

        if (html.contains("vaultterminal.gif", ignoreCase = true) ||
            html.contains("vault_term", ignoreCase = true) ||
            html.contains("Source Terminal", ignoreCase = true)
        ) {
            prefs?.setBoolean("falloutShelterTerminalAvailable", true)
        }

        val action = PlaceSync.action(url)
        when {
            action.contains("vault1") -> {
                prefs?.setBoolean("_falloutShelterVault1Used", true)
            }
            action.contains("vault3") || action.contains("spa") -> {
                if (html.contains("entire day", ignoreCase = true) ||
                    html.contains("spa", ignoreCase = true)
                ) {
                    prefs?.setBoolean(FalloutShelterRequest.SPA_USED_PREF, true)
                    prefs?.setBoolean("_falloutShelterSpaUsed", true)
                }
            }
            action.contains("vault5") || action.contains("chrono") -> {
                if (html.contains("more ominous shade of green", ignoreCase = true) ||
                    html.contains("heat death of the universe", ignoreCase = true)
                ) {
                    prefs?.setBoolean("falloutShelterChronoUsed", true)
                }
            }
            action.contains("vault8") || action.contains("reactor") || action.contains("cooling") -> {
                if (html.contains("quick dip in the cooling tank", ignoreCase = true) ||
                    html.contains("already bathed", ignoreCase = true)
                ) {
                    prefs?.setBoolean("falloutShelterCoolingTankUsed", true)
                }
            }
            action.contains("vault_term") || action.contains("terminal") -> {
                prefs?.setBoolean("falloutShelterTerminalAvailable", true)
            }
        }

        Regex("""([\d,]+)\s*Chroner""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs?.setInt("availableChroner", it) }

        ResultProcessor.processResults(false, html, null, character, preferences)
    }
}
