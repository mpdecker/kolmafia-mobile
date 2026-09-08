package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5216–5230 — thin HTTP residual registerRequest hubs (Behavioral Deepen XXII).
 */

object DimemasterRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=dimemaster", ignoreCase = true) &&
            !url.contains("whichcamp=1", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting the Dimemaster")
        return true
    }
}

object QuartersmasterRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=quartersmaster", ignoreCase = true) &&
            !url.contains("whichcamp=2", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting the Quartersmaster")
        return true
    }
}

object FlowerTradeinRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=flowertradein", ignoreCase = true) &&
            !url.contains("flowertradein", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Flower Trade-in")
        return true
    }
}

object MerchTableRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=conmerch", ignoreCase = true) &&
            !url.contains("conmerch", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Merch Table")
        return true
    }
}

object DripArmoryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=driparmory", ignoreCase = true) &&
            !url.contains("driparmory", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Drip Armory")
        return true
    }
}

object TrapperRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=trapper", ignoreCase = true) &&
            !url.contains("trapper.php", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting the Trapper")
        return true
    }
}

object ReplicaMrStoreRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=mrreplica", ignoreCase = true) &&
            !url.contains("mrreplica", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Replica Mr. Store")
        return true
    }
}

object BlackMarketRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=blackmarket", ignoreCase = true) &&
            !url.contains("blackmarket.php", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting the Black Market")
        return true
    }
}

object Crimbo25SammyRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo25_sammy", ignoreCase = true) &&
            !url.contains("crimbo25_sammy", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Crimbo25 Sammy")
        return true
    }
}

object Crimbo23ElfArmoryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo23_elf_armory", ignoreCase = true) &&
            !url.contains("crimbo23_elf_armory", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Crimbo23 Elf Armory")
        return true
    }
}

object FiveDPrinterRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=5dprinter", ignoreCase = true) &&
            !url.contains("5dprinter", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting 5D Printer")
        return true
    }
}

object PixelRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        // Desktop PixelRequest.SHOPID is "mystic"; keep legacy "pixel" alias.
        val mystic = url.contains("whichshop=mystic", ignoreCase = true)
        val pixel = url.contains("whichshop=pixel", ignoreCase = true)
        if (!mystic && !pixel) return false
        sessionLogger?.appendRawLine("Visiting The Crackpot Mystic's Shed")
        return true
    }
}

object JarlsbergRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=jarl", ignoreCase = true) &&
            !url.contains("whichshop=jarlsberg", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Jarlsberg's Cosmic Kitchen")
        return true
    }
}

object AlliedHqRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=alliedhq", ignoreCase = true) &&
            !url.contains("whichshop=twitch_alliedhq", ignoreCase = true) &&
            !url.contains("alliedhq", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Allied HQ")
        return true
    }
}

object RumpleRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=rumple", ignoreCase = true) &&
            !url.contains("rumple", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Rumple")
        return true
    }
}
