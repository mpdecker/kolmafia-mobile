package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5036–5050 — thin HTTP residual registerRequest hubs (Behavioral Deepen XIX).
 */

object Crimbo11Request {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo11.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo 2011")
        return true
    }
}

object Crimbo14Request {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo14.php", ignoreCase = true) &&
            !url.contains("whichshop=crimbo14", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Crimbo 2014 shop")
        return true
    }
}

object Crimbo16Request {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo16.php", ignoreCase = true) &&
            !url.contains("whichshop=crimbo16", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Crimbo 2016 shop")
        return true
    }
}

object Crimbo17Request {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo17.php", ignoreCase = true) &&
            !url.contains("whichshop=crimbo17", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Crimbo 2017 shop")
        return true
    }
}

/** Desktop StillRequest — still shop register. */
object StillRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=still", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting the Still")
        return true
    }
}

object SugarSheetRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("sugarsheets", ignoreCase = true) &&
            !url.contains("whichshop=sugarsheets", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Sugar Sheets")
        return true
    }
}

object StarChartRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("starchart", ignoreCase = true) &&
            !url.contains("whichshop=starchart", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Star Chart")
        return true
    }
}

object InterestingCoinRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("interesting", ignoreCase = true) &&
            !url.contains("whichshop=0", ignoreCase = true)
        ) {
            // Desktop interesting coin shop id varies; match common patterns.
            if (!url.contains("coinmaster", ignoreCase = true) ||
                !url.contains("interesting", ignoreCase = true)
            ) {
                return false
            }
        }
        if (!url.contains("interesting", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Interesting Coin shop")
        return true
    }
}

object NuggletCraftingRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=topiary", ignoreCase = true) &&
            !url.contains("topiary", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting Nugglet Crafting (topiary)")
        return true
    }
}

object SewerRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("inv_use.php", ignoreCase = true)) return false
        // chewing gum through the sewer / worthless item shuffle
        if (!url.contains("whichitem=23", ignoreCase = true) &&
            !url.contains("whichitem=24", ignoreCase = true) &&
            !url.contains("whichitem=25", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Using sewer gum")
        return true
    }
}

object ClipArtRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("campground.php", ignoreCase = true) &&
            !url.contains("skillz.php", ignoreCase = true)
        ) {
            return false
        }
        if (!url.contains("clipart", ignoreCase = true) &&
            !url.contains("summon", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Summoning clip art")
        return true
    }
}

object GnomeTinkerRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("gnomes.php", ignoreCase = true)) return false
        if (!url.contains("tinksomething", ignoreCase = true) &&
            !url.contains("action=tinker", ignoreCase = true)
        ) {
            return true // visit
        }
        sessionLogger?.appendRawLine("Gnome tinkering")
        return true
    }
}

object PhineasRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("volcanoisland.php", ignoreCase = true)) return false
        if (!url.contains("sealhide", ignoreCase = true) &&
            !url.contains("action=makestuff", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Phineas crafting")
        return true
    }
}

object TerminalExtrudeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("choice.php", ignoreCase = true)) return false
        if (!url.contains("whichchoice=1191", ignoreCase = true) &&
            !url.contains("extrude", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Terminal extrude")
        return true
    }
}

object SpacegateEquipmentRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("choice.php", ignoreCase = true)) return false
        if (!url.contains("whichchoice=1233")) return false
        sessionLogger?.appendRawLine("Spacegate equipment create")
        return true
    }
}

object ChefStaffRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("guild.php", ignoreCase = true)) return false
        if (!url.contains("makestaff", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Creating chefstaff")
        return true
    }
}
