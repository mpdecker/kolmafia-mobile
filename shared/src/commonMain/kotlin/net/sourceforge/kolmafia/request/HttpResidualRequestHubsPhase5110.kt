package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5096–5110 — thin HTTP residual registerRequest hubs (Behavioral Deepen XX).
 */

object BURTRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("inv_use.php", ignoreCase = true)) return false
        if (!url.contains("whichitem=5683")) return false
        sessionLogger?.appendRawLine("Using BURT")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("whichitem=5683")) return
        val prefs = preferences ?: return
        if (html.contains("You acquire", ignoreCase = true) ||
            html.contains("BURT", ignoreCase = true)
        ) {
            prefs.setBoolean("_burtUsed", true)
        }
    }
}

object FreeSnackRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("gamestore.php", ignoreCase = true)) return false
        if (!url.contains("freesnack", ignoreCase = true) &&
            !url.contains("action=buysnack", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Buying free snack")
        return true
    }
}

object GameShoppeRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("gamestore.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Game Shoppe")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("gamestore.php", ignoreCase = true)) return
        val prefs = preferences ?: return
        Regex("""([\d,]+)\s+Game Grid ticket""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs.setInt("availableGameGridTickets", it) }
    }
}

object ShadowForgeRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=shadowforge", ignoreCase = true) &&
            !url.contains("whichshop=shadow", ignoreCase = true) &&
            !url.contains("shadowforge", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Shadow Forge")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("whichshop=shadow", ignoreCase = true) &&
            !url.contains("shadowforge", ignoreCase = true)
        ) {
            return
        }
        val prefs = preferences ?: return
        Regex("""([\d,]+)\s+shadow (?:coin|token)""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs.setInt("availableShadowCoins", it) }
    }
}

object IsotopeSmitheryRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=isotope", ignoreCase = true) &&
            !url.contains("whichshop=elvishp1", ignoreCase = true) &&
            !url.contains("isotopesmithery", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Isotope Smithery")
        return true
    }
}

object AltarOfBonesRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("bone_altar.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Altar of Bones")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("bone_altar.php", ignoreCase = true)) return
        val prefs = preferences ?: return
        Regex("""([\d,]+)\s+bone\s+chips?""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs.setInt("availableBoneChips", it) }
    }
}

object TravelingTraderRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("traveler.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Traveling Trader")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("traveler.php", ignoreCase = true)) return
        val prefs = preferences ?: return
        Regex("""([\d,]+)\s+twinkly\s+wad""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs.setInt("availableTwinklyWads", it) }
    }
}

object CrimboCartelRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo09.php", ignoreCase = true) &&
            !url.contains("whichshop=crimbocartel", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Crimbo Cartel")
        return true
    }
}

object BigBrotherRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("monkeycastle.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Big Brother")
        return true
    }
}

object FudgeWandRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (url.contains("inv_use.php", ignoreCase = true) &&
            url.contains("whichitem=5441")
        ) {
            sessionLogger?.appendRawLine("Using fudge wand")
            return true
        }
        if (url.contains("choice.php", ignoreCase = true) &&
            url.contains("whichchoice=562")
        ) {
            sessionLogger?.appendRawLine("Fudge wand choice")
            return true
        }
        return false
    }
}

object SkeletonOfCrimboPastRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (url.contains("talktosocp=1", ignoreCase = true)) {
            sessionLogger?.appendRawLine("Talking to Skeleton of Crimbo Past")
            return true
        }
        if (url.contains("choice.php", ignoreCase = true) &&
            url.contains("whichchoice=1567")
        ) {
            sessionLogger?.appendRawLine("Skeleton of Crimbo Past choice")
            return true
        }
        return false
    }
}

object AWOLQuartermasterRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=awol", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting A.W.O.L. Quartermaster")
        return true
    }
}

object MrStoreRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("mrstore.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Mr. Store")
        return true
    }
}

object SwaggerShopRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=swagger", ignoreCase = true) &&
            !(url.contains("peevpee.php", ignoreCase = true) &&
                url.contains("place=shop", ignoreCase = true))
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Swagger Shop")
        return true
    }
}
