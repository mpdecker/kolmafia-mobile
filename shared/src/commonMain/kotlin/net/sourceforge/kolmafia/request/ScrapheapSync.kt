package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop PlaceRequest scrapheap whichplace sync (Phases 2361–2375).
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
        preferences?.setBoolean("scrapheapAvailable", true)
        ENERGY.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()?.let {
            preferences?.setInt("scrapheapEnergy", it)
            preferences?.setInt("youRobotEnergy", it)
        }
        CHRONOLITH.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()?.let {
            preferences?.setInt("_chronolithAdv", it)
        }
        val action = PlaceSync.action(url)
        when {
            action.contains("chronolith") -> {
                preferences?.setBoolean("_chronolithUsed", true)
            }
            action.contains("scavenge") || action.contains("scavenge1") -> {
                preferences?.setInt(
                    "_scrapheapScavenges",
                    preferences.getInt("_scrapheapScavenges", 0) + 1,
                )
            }
            action.contains("power") || action.contains("collect") -> {
                preferences?.setBoolean("_scrapheapPowerCollected", true)
            }
        }
        ResultProcessor.processResults(false, html, null, character, preferences)
    }
}
