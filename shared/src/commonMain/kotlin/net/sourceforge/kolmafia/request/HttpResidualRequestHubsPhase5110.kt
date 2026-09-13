package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.ItemStack
import net.sourceforge.kolmafia.shop.ShopRow

/**
 * Phases 5096–5110 — thin HTTP residual registerRequest hubs (Behavioral Deepen XX).
 * LII Track B deepens [TravelingTraderRequest.parseResponse].
 */

object BURTRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("inv_use.php", ignoreCase = true)) return false
        if (!url.contains("whichitem=5683")) return false
        sessionLogger?.appendRawLine("Using BURT")
        return true
    }

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
    ) {
        if (!url.contains("whichitem=5683") &&
            !url.contains("whichshop=burt", ignoreCase = true)
        ) {
            return
        }
        val prefs = preferences ?: return
        if (html.contains("You acquire", ignoreCase = true) ||
            html.contains("BURT", ignoreCase = true)
        ) {
            prefs.setBoolean("_burtUsed", true)
        }
        Regex("""You have ([\d,]+)\s+BURT""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { MiscShopTokenResponseParse.syncInventoryCount(inventory, MiscShopTokenResponseParse.BURT, it) }
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

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        MiscShopTokenResponseParse.parseResponse(url, html, preferences)
    }
}

object GameShoppeRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("gamestore.php", ignoreCase = true)) return false
        if (url.contains("place=cashier", ignoreCase = true)) {
            sessionLogger?.appendRawLine("Visiting Game Shoppe Cashier")
        } else {
            sessionLogger?.appendRawLine("Visiting Game Shoppe")
        }
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        XliiHttpResidualParse.parseResponse(url, html, preferences)
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

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        LegacyCoinmasterResponseParse.parseResponse(url, html, preferences)
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
    const val SHOP_KEY = CoinmasterVisitInventory.TRADER

    private val ACQUIRE_PATTERN = Regex(
        """The traveling trader is looking to acquire.*?descitem\((\d+)\).*?<b>([^<]*)</b>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val INVENTORY_PATTERN = Regex(
        """\(You have <b>([\d,]*|none)</b> on you\.\)""",
        RegexOption.IGNORE_CASE,
    )
    private val ITEM_PATTERN = Regex(
        """name=whichitem value=(\d+).*?>.*?descitem.*?(\d+).*?<b>([^<]*)</b></a></td><td>(\d+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("traveler.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Traveling Trader")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("traveler.php", ignoreCase = true)) return
        val prefs = preferences ?: return

        val acquire = ACQUIRE_PATTERN.find(html)
        if (acquire != null) {
            val descId = acquire.groupValues[1]
            val plural = acquire.groupValues[2].trim()
            prefs.setString("travelingTraderDescId", descId)
            prefs.setString("travelingTraderToken", plural)
            INVENTORY_PATTERN.find(html)?.groupValues?.getOrNull(1)?.let { raw ->
                val count = when {
                    raw.equals("none", ignoreCase = true) -> 0
                    raw.equals("one", ignoreCase = true) -> 1
                    else -> raw.replace(",", "").toIntOrNull() ?: 0
                }
                prefs.setInt("travelingTraderHave", count)
                if (plural.contains("twinkly", ignoreCase = true)) {
                    prefs.setInt("availableTwinklyWads", count)
                }
            }
        } else {
            // Legacy twinkly-wad balance line when acquire block is absent.
            Regex("""([\d,]+)\s+twinkly\s+wad""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
                ?.let { prefs.setInt("availableTwinklyWads", it) }
        }

        val rows = mutableListOf<ShopRow>()
        ITEM_PATTERN.findAll(html).forEach { match ->
            val itemId = match.groupValues[1].toIntOrNull() ?: return@forEach
            val desc = match.groupValues[2]
            val name = match.groupValues[3].trim()
            val price = match.groupValues[4].toIntOrNull() ?: return@forEach
            ItemDatabase.registerItem(itemId, name, desc)
            rows.add(
                ShopRow(
                    rowId = itemId,
                    item = ItemStack(itemId = itemId, count = 1),
                    price = price,
                ),
            )
        }
        if (rows.isNotEmpty()) {
            CoinmasterVisitInventory.replaceBuyRows(SHOP_KEY, rows)
        }
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

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean =
        CrimboHubResponseParse.parseResponse(url, html, preferences)
}

object BigBrotherRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("monkeycastle.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Big Brother")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        LegacyCoinmasterResponseParse.parseResponse(url, html, preferences)
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

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        LegacyCoinmasterResponseParse.parseResponse(url, html, preferences)
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

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        LegacyCoinmasterResponseParse.parseResponse(url, html, preferences)
    }
}

object MrStoreRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("mrstore.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Mr. Store")
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?) {
        LegacyCoinmasterResponseParse.parseResponse(url, html, preferences)
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
