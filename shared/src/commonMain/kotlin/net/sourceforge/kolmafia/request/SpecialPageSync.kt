package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Cake Arena + Bounty Hunter visit sync — deepened in Phases 3231–3290;
 * thin wrappers delegate to CakeArenaRequest / BountyHunterHunterRequest.
 */
object CakeArenaSync {
    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences? = null,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
        familiarManager: FamiliarManager? = null,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        if (!url.contains("arena.php", ignoreCase = true)) return false
        CakeArenaRequest.parseResponse(
            url, html, preferences, character, inventory, familiarManager, sessionLogger,
        )
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
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        if (!url.contains("bounty.php", ignoreCase = true)) return false
        BountyHunterHunterRequest.parseResponse(
            url, html, preferences, character, inventory, sessionLogger,
        )
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
