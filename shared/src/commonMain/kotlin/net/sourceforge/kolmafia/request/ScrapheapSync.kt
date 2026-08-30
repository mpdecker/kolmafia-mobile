package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop PlaceRequest scrapheap whichplace sync (Phases 2361–2375 + 3171–3200 deepen).
 */
object ScrapheapSync {
    private val ENERGY = Regex(
        """([\d,]+)\s*(?:units? of )?energy""",
        RegexOption.IGNORE_CASE,
    )
    private val CHRONOLITH = Regex(
        """chronolith.*?([\d,]+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        // Prefer deepened ScrapheapRequest when action is present; still handle legacy paths.
        ScrapheapRequest.parseResponse(url, html, preferences, character)
        val action = PlaceSync.action(url)
        when {
            action.contains("scavenge", ignoreCase = true) ||
                action.contains("scavenge1", ignoreCase = true) ||
                action.startsWith("sh_scrounge", ignoreCase = true) -> {
                preferences?.setInt(
                    "_scrapheapScavenges",
                    preferences.getInt("_scrapheapScavenges", 0) + 1,
                )
            }
        }
    }

    fun parseEnergyFields(
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        preferences?.setBoolean("scrapheapAvailable", true)
        ENERGY.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()?.let {
            preferences?.setInt("scrapheapEnergy", it)
            preferences?.setInt("youRobotEnergy", it)
            character?.setYouRobotEnergy(it)
        }
        CHRONOLITH.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()?.let {
            preferences?.setInt("_chronolithAdv", it)
        }
    }
}
