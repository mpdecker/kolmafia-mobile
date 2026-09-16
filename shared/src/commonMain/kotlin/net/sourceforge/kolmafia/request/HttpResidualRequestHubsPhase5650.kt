package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5636–5650 — thin HTTP residual NPC shop registerRequest hubs
 * (Behavioral Deepen XXIX). Closes the remaining shops.txt NPC rows without
 * dedicated whichshop hubs (Crimbo seasonal cafes + guild/antique leftovers).
 */

object Crimbo18CafeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo18", ignoreCase = true) ||
            url.contains("whichshop=crimbo18giftomat", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting The Crimbo Cafe (2018)")
        return true
    }
}

object Crimbo18GiftOMatRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo18giftomat", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo Town Gift-O-Mat")
        return true
    }
}

object Crimbo19CafeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo19", ignoreCase = true) ||
            url.contains("whichshop=crimbo19toys", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Visiting The Crimbo Cafe (2019)")
        return true
    }
}

object Crimbo20BlackMarketRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo20blackmarket", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Black and White and Red All Over Market")
        return true
    }
}

object Crimbo20CafeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo20cafe", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Crimbo Cafe (2020)")
        return true
    }
}

object Crimbo21CafeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo21cafe", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting The Crimbo Cafe (2021)")
        return true
    }
}

object Crimbo21OrnamentsRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo21ornaments", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Ornament Stand")
        return true
    }
}

object Crimbo25CafeRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=crimbo25_cafe", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Crimbo Cafe (2025)")
        return true
    }
}

object CyberHackMarketRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=cyber_hackmarket", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Hack Market")
        return true
    }
}

object GnoMartRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=gnomart", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Gno-Mart")
        return true
    }
}

object GuildStore1RequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=guildstore1", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Shadowy Store")
        return true
    }
}

object GuildStore2RequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=guildstore2", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Gouda's Grimoire and Grocery")
        return true
    }
}

object GuildStore3RequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=guildstore3", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Smacketeria")
        return true
    }
}

object LittleCanadiaJewelersRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=jewelers", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Little Canadia Jewelers")
        return true
    }
}

object MadelineBakingSupplyRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=madeline", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Madeline's Baking Supply")
        return true
    }
}

object NervewreckersStoreRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=nerve", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Nervewrecker's Store")
        return true
    }
}

object HugglerSnackBarRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=snackbar", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Huggler Memorial Colosseum Snack Bar")
        return true
    }
}

object TownGiftShopRequestHub {
    private val WHICHITEM_PATTERN = Regex("""[?&]whichitem=(\d+)""", RegexOption.IGNORE_CASE)
    private val HOWMANY_PATTERN = Regex("""[?&]howmany=(\d+)""", RegexOption.IGNORE_CASE)
    private const val SHOP_NAME = "The Town Gift Shop"

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("town_giftshop.php", ignoreCase = true) &&
            !url.contains("whichshop=town_giftshop", ignoreCase = true)
        ) {
            return false
        }
        if (url.contains("town_giftshop.php", ignoreCase = true) &&
            url.contains("action=buy", ignoreCase = true)
        ) {
            val itemId = WHICHITEM_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (itemId != null) {
                val quantity = HOWMANY_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: 1
                val itemName = net.sourceforge.kolmafia.data.ItemDatabase.getItemName(itemId)
                    .ifBlank { itemId.toString() }
                val price = net.sourceforge.kolmafia.data.NpcStoreDatabase.itemEntry(itemId)
                    ?.second
                    ?.price
                    ?: net.sourceforge.kolmafia.data.NpcStoreDatabase.npcPrice(itemName)
                sessionLogger?.appendRawLine(
                    "buy $quantity $itemName for $price each from $SHOP_NAME",
                )
                return true
            }
        }
        sessionLogger?.appendRawLine("Visiting Gift Shop")
        return true
    }
}

object UnclePAntiquesRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichshop=unclep", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Uncle P's Antiques")
        return true
    }
}
