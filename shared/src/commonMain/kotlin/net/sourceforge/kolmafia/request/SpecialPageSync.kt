package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager

/**
 * Cake Arena + Bounty Hunter visit sync (Phases 2301–2315).
 */
object CakeArenaSync {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences? = null,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
    ): Boolean {
        if (!url.contains("arena.php", ignoreCase = true)) return false
        preferences?.setBoolean("cakeArenaVisited", true)
        ResultProcessor.processResults(false, html, inventory, character, preferences)
        val fights = Regex("""You have (\d+) fight""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.toIntOrNull()
        if (fights != null) {
            preferences?.setInt("cakeArenaFightsLeft", fights)
        }
        return true
    }
}

object BountyHunterSync {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences? = null,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
    ): Boolean {
        if (!url.contains("bounty.php", ignoreCase = true)) return false
        preferences?.setBoolean("bountyHunterVisited", true)
        ResultProcessor.processResults(false, html, inventory, character, preferences)
        // Active bounty name if present
        Regex(
            """(?:Current Bounty|Your assignment):\s*<b>(.*?)</b>""",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            preferences?.setString("currentBountyItem", it)
        }
        return true
    }
}

/** Selective BeerPong / shrine residual glue. */
object PirateSpecialSync {
    fun parseBeerPong(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("beerpong", ignoreCase = true) &&
            !html.contains("pirate insult", ignoreCase = true)
        ) {
            return false
        }
        preferences?.setBoolean("_beerPongSeen", true)
        return true
    }

    fun parseShrine(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("shrine", ignoreCase = true) &&
            !url.contains("altarofbones", ignoreCase = true)
        ) {
            return false
        }
        preferences?.setBoolean("_shrineVisited", true)
        ResultProcessor.processResults(false, html, null, null, preferences)
        return true
    }
}
