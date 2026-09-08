package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5391–5405 — thin HTTP residual registerRequest hubs (Behavioral Deepen XXV).
 */

object SeptEmberCenserRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=september", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Sept-Ember Censer")
        return true
    }
}

object SpinMasterLatheRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=lathe", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Your SpinMaster™ lathe")
        return true
    }
}

object ShoreGiftShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=shore", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Shore, Inc. Gift Shop")
        return true
    }
}

object InternetMemeShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=bacon", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Internet Meme Shop")
        return true
    }
}

object TerrifiedEagleInnRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=dv", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Terrified Eagle Inn")
        return true
    }
}

object TinkeringBenchRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=wereprofessor_tinker", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Tinkering Bench")
        return true
    }
}

object VendingMachineRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=damachine", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Vending Machine")
        return true
    }
}

object FixodentRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=fixodent", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Craft with Teeth")
        return true
    }
}

object PlumberGearRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=mariogear", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Mushroom District Gear Shop")
        return true
    }
}

object PlumberItemRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=marioitems", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Mushroom District Item Shop")
        return true
    }
}

object PokemporiumRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=pokefam", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Pokémporium")
        return true
    }
}

object FancyDanRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=olivers", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Fancy Dan the Cocktail Man")
        return true
    }
}

object NinaStoreRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=nina", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Niña Store")
        return true
    }
}

object YeNeweSouvenirShoppeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=shakeshop", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Ye Newe Souvenir Shoppe")
        return true
    }
}

object XOShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=xo", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting XO Shop")
        return true
    }
}

object SpantRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=spant", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Spant Bit Assembly")
        return true
    }
}
