package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop PlaceRequest.parseResponse / getAdventuresUsed hub (Phases 2361–2390).
 */
object PlaceSync {
    fun whichPlace(url: String): String? {
        val m = Regex("""whichplace=([^&]+)""", RegexOption.IGNORE_CASE).find(url) ?: return null
        return m.groupValues[1].lowercase()
    }

    fun action(url: String): String =
        url.substringAfter("action=", "").substringBefore('&').lowercase()

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
    ) {
        if (!url.contains("place.php", ignoreCase = true)) return
        val place = whichPlace(url) ?: return
        when (place) {
            "chateau" -> ChateauSync.parseResponse(url, html, preferences, character)
            "campaway" -> CampAwaySync.parseResponse(url, html, preferences, character)
            "falloutshelter" -> FalloutShelterSync.parseResponse(url, html, preferences, character)
            "scrapheap" -> ScrapheapSync.parseResponse(url, html, preferences, character)
            "rabbithole" -> {
                preferences?.setBoolean("rabbitHoleVisited", true)
                ResultProcessor.processResults(false, html, inventory, character, preferences)
            }
            "arcade" -> {
                preferences?.setBoolean("arcadeVisited", true)
                Regex("""([\d,]+) tickets?""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
                    ?.let { preferences?.setInt("arcadeGameTickets", it) }
            }
            "woods" -> {
                if (html.contains("getaway", ignoreCase = true) ||
                    html.contains("campsite", ignoreCase = true)
                ) {
                    preferences?.setBoolean("getawayCampsiteUnlocked", true)
                }
            }
            "mountains" -> {
                if (html.contains("chateau", ignoreCase = true)) {
                    preferences?.setBoolean("chateauAvailable", true)
                }
                if (html.contains("snojo", ignoreCase = true)) {
                    preferences?.setBoolean("snojoAvailable", true)
                }
                if (html.contains("spacegate", ignoreCase = true)) {
                    preferences?.setBoolean("spacegateAlways", true)
                }
            }
            else -> {
                // Existing specialized syncs (twitch/crimbo/spelunky/batman) stay in visit hooks.
            }
        }
    }

    /**
     * Desktop PlaceRequest.getAdventuresUsed subset.
     */
    fun getAdventuresUsed(
        url: String,
        freeRestsRemaining: Int = 0,
        preferences: Preferences? = null,
    ): Int {
        if (!url.contains("place.php", ignoreCase = true)) return 0
        val place = whichPlace(url) ?: return 0
        val act = action(url)
        return when (place) {
            "campaway" -> {
                if (act.contains("tent") || act.contains("rest")) {
                    if (freeRestsRemaining > 0) 0 else 1
                } else 0
            }
            "chateau" -> {
                when {
                    act.contains("painting") &&
                        preferences?.getBoolean("_chateauMonsterFought", false) != true -> 1
                    act.contains("rest") -> if (freeRestsRemaining > 0) 0 else 1
                    else -> 0
                }
            }
            "falloutshelter" -> if (act.contains("vault1") || act == "vault1") 1 else 0
            "bugbearship" -> if (act.contains("bb_bridge")) 1 else 0
            "nstower" -> {
                if (act.contains("ns_01_crowd") || act.contains("ns_0") ||
                    act.contains("monster") || act.contains("door")
                ) 1 else 0
            }
            "manor4" -> if (act.contains("chamber") || act.contains("lord")) 1 else 0
            "ioty2014_wolf" -> if (act.contains("houserun")) 3 else 0
            else -> 0
        }
    }
}
