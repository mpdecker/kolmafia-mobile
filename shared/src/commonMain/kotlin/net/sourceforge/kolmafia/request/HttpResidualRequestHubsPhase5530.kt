package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5516–5530 — thin HTTP residual registerRequest hubs (Behavioral Deepen XXVII).
 */

object InfernoDiscoRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=infernodisco", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Disco GiftCo")
        return true
    }
}

object WarbearBoxRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=warbear", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Warbear Black Box")
        return true
    }
}

object WalMartRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=glaciest", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Wal-Mart")
        return true
    }
}

object ToxicChemistryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=toxic", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Toxic Chemistry")
        return true
    }
}

object FishboneryRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=fishbones", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Freshwater Fishbonery")
        return true
    }
}

object DinostaurRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=dino", ignoreCase = true) ||
            url.contains("whichshop=dinobone", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Dino World Gift Shop (The Dinostaur)")
        return true
    }
}

object DinoBoneFragmentRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=dinobone", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Dino Bone Fragment Assembly")
        return true
    }
}

object BeerGardenRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=beergarden", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Beer Garden")
        return true
    }
}

object ShoeRepairRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=shoeshop", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Legitimate Shoe Repair, Inc.")
        return true
    }
}

object WetCrapForSaleRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=sandpenny", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Wet Crap For Sale")
        return true
    }
}

object PorkElfPotteryShardRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=potsherd", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Pork Elf Pottery Shard Assembly")
        return true
    }
}

object WinterGardenRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=snowgarden", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Winter Gardening")
        return true
    }
}

object UsingYourShowerThoughtsRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=showerthoughts", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Using your Shower Thoughts")
        return true
    }
}

object ThankShopRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=thankshop", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting A traveling Thanksgiving salesman")
        return true
    }
}

object SliemceRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=voteslime", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Mad Sliemce")
        return true
    }
}

object DollHawkerRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=elvishp2", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Dollhawker's Emporium")
        return true
    }
}

object LunarLunchRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=elvishp3", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Lunar Lunch-o-Mat")
        return true
    }
}

object TwitchJoustingRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=twitch_jousting", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Renaissance Gift Shop")
        return true
    }
}

object PrimordialSoupKitchenRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=twitchsoup", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Primordial Soup Kitchen")
        return true
    }
}

object KringleRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo19toys", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The H. M. S. Kringle's Workshop")
        return true
    }
}

object LandfillDetritus2015RequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=detritus2015", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Landfill Detritus from 2015 Assembly")
        return true
    }
}
