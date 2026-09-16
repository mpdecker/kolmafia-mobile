package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shop.TimeTowerSync

/**
 * Phases 5276–5290 — thin HTTP residual registerRequest hubs (Behavioral Deepen XXIII).
 * LV Track C: Chroner coin-shop visit parse (TimeTower + token inv via [MiscShopTokenResponseParse]).
 *
 * Siblings `nina` / `shakeshop` / `shoeshop` have no register-only hubs here; `shop.php` already
 * runs [MiscShopTokenResponseParse] and coinmaster [ChronerShopSync.applyVisitShop] for all
 * [TimeTowerSync.CHRONER_SHOP_IDS].
 */

private val WHICH_SHOP = Regex("""whichshop=([^&]+)""", RegexOption.IGNORE_CASE)

private fun parseChronerRegisterHubResponse(
    url: String?,
    html: String,
    preferences: Preferences?,
    inventory: InventoryManager?,
    shopIds: Set<String>,
): Boolean {
    if (url.isNullOrBlank() || preferences == null) return false
    if (!url.contains("shop.php", ignoreCase = true)) return false
    val shopId = WHICH_SHOP.find(url)?.groupValues?.getOrNull(1)?.lowercase() ?: return false
    if (shopId !in shopIds) return false
    if (shopId in TimeTowerSync.CHRONER_SHOP_IDS) {
        TimeTowerSync.syncFromChronerShopHtml(html, preferences)
    }
    MiscShopTokenResponseParse.parseResponse(url, html, preferences, inventory)
    return true
}

object TicketCounterRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=arcade", ignoreCase = true) &&
            !url.contains("arcade.php", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Ticket Counter")
        return true
    }
}

object DinseyCompanyStoreRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=landfillstore", ignoreCase = true) &&
            !url.contains("landfillstore", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Dinsey Company Store")
        return true
    }
}

object EdShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=edunder_shopshop", ignoreCase = true) &&
            !url.contains("edunder_shopshop", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Ed's Shop")
        return true
    }
}

object PrecinctRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=detective", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Precinct")
        return true
    }
}

object SpacegateFabricationRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=spacegate", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Spacegate Fabrication")
        return true
    }
}

object FDKOLRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=fdkol", ignoreCase = true) &&
            !url.contains("fdkol", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting FDKOL")
        return true
    }
}

object MrStore2002RequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=mrstore2002", ignoreCase = true) &&
            !url.contains("mrstore2002", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Mr. Store 2002")
        return true
    }
}

object AppleStoreRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=applestore", ignoreCase = true) &&
            !url.contains("applestore", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Apple Store")
        return true
    }

    fun parseResponse(
        url: String?,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
    ): Boolean = parseChronerRegisterHubResponse(url, html, preferences, inventory, setOf("applestore"))
}

object BrogurtRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=sbb_brogurt", ignoreCase = true) &&
            !url.contains("brogurt", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Brogurt")
        return true
    }
}

object GMartRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=glover", ignoreCase = true) &&
            !url.contains("whichshop=gmart", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting GMart")
        return true
    }
}

object RubeeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=fantasyrealm", ignoreCase = true) &&
            !url.contains("fantasyrealm", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting FantasyRealm Rubee Shop")
        return true
    }
}

object YourCampfireRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=campfire", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Your Campfire")
        return true
    }
}

object KiwiKwikiMartRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=kiwi", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Kiwi Kwiki Mart")
        return true
    }
}

object NeandermallRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=caveshop", ignoreCase = true) &&
            !url.contains("caveshop", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Neandermall")
        return true
    }

    fun parseResponse(
        url: String?,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
    ): Boolean = parseChronerRegisterHubResponse(url, html, preferences, inventory, setOf("caveshop"))
}

object CrimboCartelLegacyRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbco", ignoreCase = true) &&
            !url.contains("crimbcogiftshop", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting CRIMBCO Gift Shop")
        return true
    }
}
