package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5456–5470 — thin HTTP residual registerRequest hubs (Behavioral Deepen XXVI).
 */

object GuzzlrRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=guzzlr", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Guzzlr Company Store Website")
        return true
    }
}

object GrandmaRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=grandma", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Grandma Sea Monkey")
        return true
    }
}

object ArmoryAndLeggeryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=armory", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Armory & Leggery")
        return true
    }
}

object CosmicRaysBazaarRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=exploathing", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Cosmic Ray's Bazaar")
        return true
    }
}

object GeneticFiddlingRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=mutate", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Genetic Fiddling")
        return true
    }
}

object AirportDutyFreeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=airport", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Elemental Duty Free, Inc.")
        return true
    }
}

object ChemiCorpRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=batman_chemicorp", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting ChemiCorp")
        return true
    }
}

object GotporkOrphanageRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=batman_orphanage", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Gotpork Orphanage")
        return true
    }
}

object GotporkPDRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=batman_pd", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Gotpork P. D.")
        return true
    }
}

object BuffJimmyRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=sbb_jimmy", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Buff Jimmy's Souvenir Shop")
        return true
    }
}

object TacoDanRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=sbb_taco", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Taco Dan's Taco Stand")
        return true
    }
}

object ShawarmaInitiativeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=si_shop1", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The SHAWARMA Initiative")
        return true
    }
}

object CanteenRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=si_shop2", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Canteen")
        return true
    }
}

object SpacegateArmoryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=si_shop3", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Armory")
        return true
    }
}

object LtTRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=ltt", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting LT&T Gift Shop")
        return true
    }
}

object CindyRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=cindy", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Paul's Boutique")
        return true
    }
}
