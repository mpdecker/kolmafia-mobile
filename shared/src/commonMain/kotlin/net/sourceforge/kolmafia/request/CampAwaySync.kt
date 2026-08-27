package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop PlaceRequest campaway whichplace sync (Phases 2361–2375).
 */
object CampAwaySync {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        preferences?.setBoolean("getawayCampsiteUnlocked", true)
        val action = PlaceSync.action(url)
        when {
            action.contains("tent") || action.contains("rest") -> {
                preferences?.setInt("timesRested", (preferences.getInt("timesRested", 0) + 1))
                if (html.contains("free", ignoreCase = true) ||
                    html.contains("didn't cost", ignoreCase = true)
                ) {
                    preferences?.setInt(
                        "_freeRestsUsed",
                        preferences.getInt("_freeRestsUsed", 0) + 1,
                    )
                }
                preferences?.setBoolean("_campAwayTentRested", true)
            }
            action.contains("cloud") || html.contains("cloud bun", ignoreCase = true) -> {
                preferences?.setBoolean("_campAwayCloudBuffUsed", true)
                preferences?.setInt(
                    CampAwayRequest.CLOUD_BUFFS_PREF,
                    preferences.getInt(CampAwayRequest.CLOUD_BUFFS_PREF, 0) + 1,
                )
            }
            action.contains("smile") || html.contains("smile of", ignoreCase = true) -> {
                preferences?.setBoolean("_campAwaySmileBuffUsed", true)
            }
        }
        ResultProcessor.processResults(false, html, null, character, preferences)
    }
}
