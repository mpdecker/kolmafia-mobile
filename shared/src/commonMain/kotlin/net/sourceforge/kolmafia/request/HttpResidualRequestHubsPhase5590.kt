package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5576–5590 — thin HTTP residual NPC shop registerRequest hubs
 * (Behavioral Deepen XXVIII). Coinmaster/shop residual is closed; this batch
 * covers high-traffic NPC/NPCCOIN shops from shops.txt.
 */

object DocGalaktikShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=doc", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Doc Galaktik's Medicine Show")
        return true
    }
}

object MeatsmithShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=meatsmith", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Meatsmith's Shop")
        return true
    }
}

object MayoClinicShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=mayoclinic", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Mayo Clinic")
        return true
    }
}

object HiddenTavernShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=hiddentavern", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Hidden Tavern")
        return true
    }
}

object HippyStoreRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=hippy", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Hippy Store (Pre-War)")
        return true
    }
}

object FwShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=fwshop", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Clan Underground Fireworks Shop")
        return true
    }
}

object DripCafeteriaShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=dripcafeteria", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Drip Institute Cafeteria")
        return true
    }
}

object Vault1ShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=vault1", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Fallout Shelter Medical Supply")
        return true
    }
}

object Vault2ShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=vault2", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Fallout Shelter Electronics Supply")
        return true
    }
}

object Vault3ShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=vault3", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Underground Record Store")
        return true
    }
}

object GeneralStoreRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=generalstore", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The General Store")
        return true
    }
}

object GnollShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=gnoll", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Degrassi Knoll Bakery and Hardware Store")
        return true
    }
}

object BartenderShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=bartender", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Typical Tavern")
        return true
    }
}

object BartlebysShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=bartlebys", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Barrrtleby's Barrrgain Books")
        return true
    }
}

object WildfireShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=wildfire", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting FDKOL Auxiliary")
        return true
    }
}

object WhiteCitadelShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=whitecitadel", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting White Citadel")
        return true
    }
}

object KnobDispensaryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=knobdisp", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Knob Dispensary")
        return true
    }
}

object BugbearBakeryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=bugbear", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Bugbear Bakery")
        return true
    }
}

object ChinatownShopsRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=chinatown", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Chinatown Shops")
        return true
    }
}

object TweedleporiumRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=tweedle", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Tweedleporium")
        return true
    }
}
