package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [net.sourceforge.kolmafia.request.concoction.BurningNewspaperRequest] —
 * choice 1277 burning newspaper craft register.
 */
object BurningNewspaperRequest {
    private const val CHOICE = 1277

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("choice.php", ignoreCase = true)) return false
        if (!url.contains("whichchoice=$CHOICE")) return false
        val option = Regex("""[?&]option=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()
        if (option != null && option in 1..5) {
            sessionLogger?.appendRawLine("Creating burning newspaper gear (option $option)")
        }
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("whichchoice=$CHOICE")) return false
        if (!html.contains("You acquire", ignoreCase = true)) return false
        preferences?.setBoolean("_burningNewspaperCrafted", true)
        return true
    }
}

/**
 * Desktop [net.sourceforge.kolmafia.request.concoction.MeteoroidRequest] —
 * choice 1264 meteoroid craft register.
 */
object MeteoroidRequest {
    private const val CHOICE = 1264

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("choice.php", ignoreCase = true)) return false
        if (!url.contains("whichchoice=$CHOICE")) return false
        val option = Regex("""[?&]option=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()
        if (option != null && option in 1..6) {
            sessionLogger?.appendRawLine("Creating meteoroid gear (option $option)")
        }
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("whichchoice=$CHOICE")) return false
        if (!html.contains("You acquire", ignoreCase = true)) return false
        preferences?.setBoolean("_meteoroidCrafted", true)
        return true
    }
}

/**
 * Desktop [net.sourceforge.kolmafia.request.concoction.GrubbyWoolRequest] —
 * choice 1490 grubby wool craft register.
 */
object GrubbyWoolRequest {
    private const val CHOICE = 1490

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("choice.php", ignoreCase = true)) return false
        if (!url.contains("whichchoice=$CHOICE")) return false
        val option = Regex("""[?&]option=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()
        if (option != null && option in 1..6) {
            sessionLogger?.appendRawLine("Creating grubby wool gear (option $option)")
        }
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("whichchoice=$CHOICE")) return false
        if (!html.contains("You acquire", ignoreCase = true)) return false
        preferences?.setBoolean("_grubbyWoolCrafted", true)
        return true
    }
}

/**
 * Desktop Crimbo05/06/07 CreateItemRequest hubs — holiday uncle create register.
 */
object Crimbo05Request {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo_uncle.php", ignoreCase = true)) return false
        val itemId = Regex("""[?&]whichitem=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        val qty = Regex("""[?&]quantity=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        sessionLogger?.appendRawLine("Create $qty item #$itemId at Crimbo uncle (2005)")
        return true
    }
}

object Crimbo06Request {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo06.php", ignoreCase = true) &&
            !url.contains("crimbo_factory.php", ignoreCase = true)
        ) {
            return false
        }
        val itemId = Regex("""[?&]whichitem=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return true
        sessionLogger?.appendRawLine("Create item #$itemId at Crimbo 2006")
        return true
    }
}

object Crimbo07Request {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo07.php", ignoreCase = true)) return false
        val itemId = Regex("""[?&]whichitem=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()
        if (itemId != null) {
            sessionLogger?.appendRawLine("Create item #$itemId at Crimbo 2007")
        }
        return true
    }
}

/**
 * Desktop [AutoSellRequest.registerRequest] — sellstuff.php session-log register.
 * Response parse remains in [AutosellSync].
 */
object AutoSellRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("sellstuff.php", ignoreCase = true) &&
            !url.contains("sellstuff_ugly.php", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Autoselling items")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("sellstuff.php", ignoreCase = true) &&
            !url.contains("sellstuff_ugly.php", ignoreCase = true)
        ) {
            return false
        }
        val prefs = preferences ?: return html.contains("You sell", ignoreCase = true)
        Regex("""You gain ([\d,]+) Meat""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs.setInt("_lastAutosellMeat", it) }
        if (html.contains("You sell", ignoreCase = true) ||
            html.contains("You gain", ignoreCase = true)
        ) {
            prefs.setBoolean("_autosellSucceeded", true)
            return true
        }
        return false
    }
}
