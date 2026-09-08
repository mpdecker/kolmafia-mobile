package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5156–5170 — thin HTTP residual registerRequest hubs (Behavioral Deepen XXI).
 */

object SendGiftRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("town_sendgift.php", ignoreCase = true) &&
            !url.contains("sendmessage.php", ignoreCase = true)
        ) {
            return false
        }
        if (!url.contains("towngift", ignoreCase = true) &&
            !url.contains("action=send", ignoreCase = true) &&
            !url.contains("sendgift", ignoreCase = true)
        ) {
            // still log town_sendgift visits
            if (!url.contains("town_sendgift.php", ignoreCase = true)) return false
        }
        sessionLogger?.appendRawLine("Sending gift")
        return true
    }
}

object SendMailRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("sendmessage.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Sending kmail")
        return true
    }
}

object GourdRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("town_right.php", ignoreCase = true) &&
            !url.contains("gourd", ignoreCase = true)
        ) {
            return false
        }
        if (!url.contains("gourd", ignoreCase = true) &&
            !url.contains("action=gourd", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting the Gourd")
        return true
    }
}

object FriarRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("friars.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting the Deep Fat Friars")
        return true
    }
}

object FamiliarRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("familiar.php", ignoreCase = true) &&
            !url.contains("familiars.php", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Managing familiars")
        return true
    }
}

object Crimbo20BoozeRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo20booze", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo20 Booze")
        return true
    }
}

object Crimbo20FoodRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo20food", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo20 Food")
        return true
    }
}

object Crimbo20CandyRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo20candy", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo20 Candy")
        return true
    }
}

object DedigitizerRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=dedigitizer", ignoreCase = true) &&
            !url.contains("whichshop=cyber_dedigitizer", ignoreCase = true) &&
            !url.contains("dedigitizer", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Dedigitizer")
        return true
    }
}

object BatFabricatorRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=batman_cave", ignoreCase = true) &&
            !url.contains("batfabricator", ignoreCase = true) &&
            !url.contains("whichshop=batman", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Bat Fabricator")
        return true
    }
}

object DiscoGiftCoRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=cgold", ignoreCase = true) &&
            !url.contains("whichshop=infernodisco", ignoreCase = true) &&
            !url.contains("discogift", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Disco GiftCo")
        return true
    }
}

object RenaissanceGiftShopRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=chateau", ignoreCase = true) &&
            !url.contains("whichshop=rsg", ignoreCase = true) &&
            !url.contains("renaissance", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Renaissance Gift Shop")
        return true
    }
}

object SummoningChamberRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("manor3.php", ignoreCase = true) &&
            !url.contains("summoningchamber", ignoreCase = true)
        ) {
            return false
        }
        if (!url.contains("action=summon", ignoreCase = true) &&
            !url.contains("place=chamber", ignoreCase = true) &&
            !url.contains("summoning", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Summoning Chamber")
        return true
    }
}

object SafetyShelterRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("place.php", ignoreCase = true)) return false
        if (!url.contains("whichplace=falloutshelter", ignoreCase = true) &&
            !url.contains("falloutshelter", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Fallout Shelter")
        return true
    }
}
