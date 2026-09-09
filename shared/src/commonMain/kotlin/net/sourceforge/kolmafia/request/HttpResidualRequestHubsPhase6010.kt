package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5989–6002 — RequestLogger residual claim hubs (Behavioral Deepen XXXV).
 * Named session-log lines for thin typed requests that previously fell through
 * to generic place/shop visit messages.
 */
object GrandpaRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("monkeycastle.php", ignoreCase = true)) return false
        if (!url.contains("grandpastory", ignoreCase = true) &&
            !url.contains("who=4", ignoreCase = true)
        ) {
            return false
        }
        val topic = RequestLogger.queryParamForHub(url, "topic")
        val message = if (topic.isNullOrBlank()) {
            "Asking Grandpa about something"
        } else {
            "Asking Grandpa about $topic"
        }
        RequestLogger.updateSessionLog(message, sessionLogger)
        return true
    }
}

object PortalRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("campground.php", ignoreCase = true)) return false
        if (!url.contains("elvibratoportal", ignoreCase = true)) return false
        RequestLogger.updateSessionLog("Charging the El Vibrato portal", sessionLogger)
        return true
    }
}

object TutorialRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!TutorialRequest.registerRequest(url)) return false
        RequestLogger.updateSessionLog("Visiting the Toot Oriole", sessionLogger)
        return true
    }
}

object HashingViseRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("inv_use.php", ignoreCase = true) &&
            !url.contains("choice.php", ignoreCase = true)
        ) {
            return false
        }
        // Hashing vise item use / choice 1484.
        if (!url.contains("whichitem=10790", ignoreCase = true) &&
            !url.contains("whichchoice=1484", ignoreCase = true)
        ) {
            return false
        }
        RequestLogger.updateSessionLog("Using the hashing vise", sessionLogger)
        return true
    }
}

object PottedTeaTreeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("campground.php", ignoreCase = true) &&
            !url.contains("choice.php", ignoreCase = true)
        ) {
            return false
        }
        if (!url.contains("teatree", ignoreCase = true) &&
            !url.contains("whichchoice=1304", ignoreCase = true)
        ) {
            return false
        }
        RequestLogger.updateSessionLog("Visiting the potted tea tree", sessionLogger)
        return true
    }
}

object PizzaCubeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!PizzaCubeRequest.isPizzaUrl(url)) return false
        RequestLogger.updateSessionLog("Cooking pizza in the pizza cube", sessionLogger)
        return true
    }
}

object UntinkerRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("forestvillage.php", ignoreCase = true) &&
            !url.contains("inv_use.php", ignoreCase = true)
        ) {
            return false
        }
        if (url.contains("forestvillage.php", ignoreCase = true) &&
            !url.contains("action=untinker", ignoreCase = true)
        ) {
            return false
        }
        if (url.contains("inv_use.php", ignoreCase = true) &&
            !url.contains("action=screw", ignoreCase = true)
        ) {
            return false
        }
        val item = RequestLogger.queryParamForHub(url, "whichitem")
        val message = if (item.isNullOrBlank()) "untinker" else "untinker item #$item"
        RequestLogger.updateSessionLog(message, sessionLogger)
        return true
    }
}

object PulverizeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("craft.php", ignoreCase = true) &&
            !url.contains("pulverize", ignoreCase = true)
        ) {
            return false
        }
        if (!url.contains("action=pulverize", ignoreCase = true) &&
            !url.contains("mode=pulverize", ignoreCase = true)
        ) {
            return false
        }
        val item = RequestLogger.queryParamForHub(url, "smashitem")
            ?: RequestLogger.queryParamForHub(url, "whichitem")
        val message = if (item.isNullOrBlank()) "pulverize" else "pulverize item #$item"
        RequestLogger.updateSessionLog(message, sessionLogger)
        return true
    }
}

object CurseRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!CurseRequest.registerRequest(url)) return false
        val target = RequestLogger.queryParamForHub(url, "playerid")
            ?: RequestLogger.queryParamForHub(url, "targetplayer")
        val message = if (target.isNullOrBlank()) "curse" else "curse player $target"
        RequestLogger.updateSessionLog(message, sessionLogger)
        return true
    }
}

object DigRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        return DigRequest.registerRequest(url, sessionLogger)
    }
}

object CampAwayRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("place.php", ignoreCase = true) &&
            !url.contains("campground.php", ignoreCase = true)
        ) {
            return false
        }
        if (!url.contains("campaway", ignoreCase = true) &&
            !url.contains("whichplace=campaway", ignoreCase = true)
        ) {
            return false
        }
        RequestLogger.updateSessionLog("Visiting your Getaway Campsite", sessionLogger)
        return true
    }
}

object HeyDezeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        return HeyDezeRequest.registerRequest(url, sessionLogger)
    }
}
