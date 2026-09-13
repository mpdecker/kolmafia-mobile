package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Phases 6331–6350 — misc shop/token `parseResponse` batch (Behavioral Deepen XLI).
 * LIII: bacon / G / boutique / blackmarket / SI.
 * LIV Track A: Volcoino / Ka / Driplet / yeti / shore scrip / IoTM / Batfellow / LTT /
 * plumber / Kruegerand / dino / vending / Beach Bucks fan-in / arcade ticket inventory.
 */
object MiscShopTokenResponseParse {

    const val BACON_ITEM = 8763
    const val G_ITEM = 9909
    const val ODD_SILVER_COIN = 7144
    const val PRICELESS_DIAMOND = 7221
    const val COINSPIRACY = 7769
    const val VOLCOINO = 8426
    const val KA_COIN = 7966
    const val DRIPLET = 10443
    const val YETI_FUR = 388
    const val SHIP_TRIP_SCRIP = 6725
    const val BEACH_BUCK = 7429
    const val WARBEAR_WHOSIT = 6913
    const val TOXIC_GLOBULE = 8218
    const val FRESHWATER_FISHBONE = 7651
    const val GLOB_OF_WET_PAPER = 11885
    const val SAND_PENNY = 11961
    const val KIDNAPPED_ORPHAN = 8803
    const val INCRIMINATING_EVIDENCE = 8801
    const val DANGEROUS_CHEMICALS = 8802
    const val BUFFALO_DIME = 8895
    const val PLUMBER_COIN = 10454
    const val KRUEGERAND = 6433
    const val DINODOLLAR = 10944
    const val FAT_LOOT_TOKEN = 5221
    const val GG_TICKET = 4622
    const val BURT = 5683
    const val CRIMBCO_SCRIP = 4854
    const val REPLICA_MR_ACCESSORY = 11189

    private val ARCADE_TICKET = Regex(
        """You currently have ([\d,]+) Game Grid(?: redemption)? tickets?""",
        RegexOption.IGNORE_CASE,
    )
    private val SNACK_VOUCHER = Regex(
        """You have ([\d,]+) free snack voucher""",
        RegexOption.IGNORE_CASE,
    )
    private val FDKOL = Regex(
        """<td>([\d,]+) FDKOL commendation""",
        RegexOption.IGNORE_CASE,
    )
    private val RUBEE = Regex(
        """<td>([\d,]+) Rubees?(?:&trade;|™)?""",
        RegexOption.IGNORE_CASE,
    )
    private val BEACH_BUCK_PAT = Regex(
        """<td>([\d,]+) Beach Bucks?""",
        RegexOption.IGNORE_CASE,
    )
    private val FUN_FUNDS = Regex(
        """<td>([\d,]+) FunFunds""",
        RegexOption.IGNORE_CASE,
    )
    private val WALMART = Regex(
        """<td>([\d,]+) Wal[- ]?Mart gift certificate""",
        RegexOption.IGNORE_CASE,
    )
    private val WORD_NUMBERS = mapOf(
        "no" to 0, "a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3,
        "four" to 4, "five" to 5, "six" to 6, "seven" to 7, "eight" to 8,
        "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
    )

    private data class InvRule(
        val shops: List<String>,
        val pattern: Regex,
        val itemId: Int,
        val pref: String? = null,
        val wordCount: Boolean = false,
    )

    private val INV_RULES = listOf(
        InvRule(listOf("bacon"), Regex("""([\d,]+)\s+BACON""", RegexOption.IGNORE_CASE), BACON_ITEM),
        InvRule(listOf("glover"), Regex("""([\d,]+)\s+G\b""", RegexOption.IGNORE_CASE), G_ITEM),
        InvRule(
            listOf("cindy", "boutique"),
            Regex("""<td>([\d,]+)\s+odd silver coin""", RegexOption.IGNORE_CASE),
            ODD_SILVER_COIN,
        ),
        InvRule(
            listOf("blackmarket"),
            Regex("""<td>([\d,]+)\s+priceless diamond""", RegexOption.IGNORE_CASE),
            PRICELESS_DIAMOND,
        ),
        InvRule(
            listOf("si_shop", "shawarma", "canteen", "thearmory"),
            Regex("""<td>([\d,]+)\s+Coins?-spiracy""", RegexOption.IGNORE_CASE),
            COINSPIRACY,
        ),
        InvRule(
            listOf("infernodisco", "cgold"),
            Regex("""<td>([\d,]+)\s+Volcoino""", RegexOption.IGNORE_CASE),
            VOLCOINO,
        ),
        InvRule(
            listOf("edunder_shopshop", "edshop"),
            Regex("""<td>([\d,]+)\s+Ka coin""", RegexOption.IGNORE_CASE),
            KA_COIN,
        ),
        InvRule(
            listOf("driparmory"),
            Regex("""<td>([\d,]+)\s+Driplet""", RegexOption.IGNORE_CASE),
            DRIPLET,
        ),
        InvRule(
            listOf("trapper"),
            Regex("""([\d,]+)\s+yeti fur""", RegexOption.IGNORE_CASE),
            YETI_FUR,
        ),
        InvRule(
            listOf("shore"),
            Regex("""([\d,]+)\s+Shore Inc\.?\s*Ship Trip Scrip""", RegexOption.IGNORE_CASE),
            SHIP_TRIP_SCRIP,
        ),
        InvRule(
            listOf("sbb_brogurt", "brogurt", "sbb_jimmy", "sbb_taco", "buffjimmy", "tacodan"),
            BEACH_BUCK_PAT,
            BEACH_BUCK,
            pref = "availableBeachBucks",
        ),
        InvRule(
            listOf("warbear"),
            Regex("""<td>([\d,]+)\s+warbear whosit""", RegexOption.IGNORE_CASE),
            WARBEAR_WHOSIT,
        ),
        InvRule(
            listOf("toxic"),
            Regex("""<td>([\d,]+)\s+toxic globule""", RegexOption.IGNORE_CASE),
            TOXIC_GLOBULE,
        ),
        InvRule(
            listOf("fishbones"),
            Regex("""<td>([\d,]+)\s+freshwater fishbone""", RegexOption.IGNORE_CASE),
            FRESHWATER_FISHBONE,
        ),
        InvRule(
            listOf("showerthoughts"),
            Regex("""<td>([\d,]+)\s+globs? of wet paper""", RegexOption.IGNORE_CASE),
            GLOB_OF_WET_PAPER,
        ),
        InvRule(
            listOf("sandpenny"),
            Regex("""<td>([\d,]+)\s+sand penn""", RegexOption.IGNORE_CASE),
            SAND_PENNY,
        ),
        InvRule(
            listOf("batman_orphanage"),
            Regex("""<td>([\d,]+)\s+kidnapped orphan""", RegexOption.IGNORE_CASE),
            KIDNAPPED_ORPHAN,
        ),
        InvRule(
            listOf("batman_pd"),
            Regex("""<td>([\d,]+)\s+incriminating evidence""", RegexOption.IGNORE_CASE),
            INCRIMINATING_EVIDENCE,
        ),
        InvRule(
            listOf("batman_chemicorp"),
            Regex("""<td>([\d,]+)\s+dangerous chemicals""", RegexOption.IGNORE_CASE),
            DANGEROUS_CHEMICALS,
        ),
        InvRule(
            listOf("ltt"),
            Regex("""<td>([\d,]+)\s+buffalo dime""", RegexOption.IGNORE_CASE),
            BUFFALO_DIME,
        ),
        InvRule(
            listOf("mariogear", "marioitems"),
            Regex("""([\d,]+)\s+coin""", RegexOption.IGNORE_CASE),
            PLUMBER_COIN,
        ),
        InvRule(
            listOf("dv"),
            Regex("""<td>(\w+)\s+Freddy Kruegerand""", RegexOption.IGNORE_CASE),
            KRUEGERAND,
            wordCount = true,
        ),
        InvRule(
            listOf("dino"),
            Regex("""<td>([\d,]+)\s+Dinodollar""", RegexOption.IGNORE_CASE),
            DINODOLLAR,
        ),
        InvRule(
            listOf("damachine"),
            Regex("""([\d,]+)\s+fat loot token""", RegexOption.IGNORE_CASE),
            FAT_LOOT_TOKEN,
        ),
        InvRule(
            listOf("mrreplica"),
            Regex("""<td>([\d,]+)\s+Replica Mr\.?\s*Accessor""", RegexOption.IGNORE_CASE),
            REPLICA_MR_ACCESSORY,
        ),
    )

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
    ): Boolean {
        val prefs = preferences ?: return false
        return when {
            url.contains("whichshop=arcade", ignoreCase = true) ||
                (url.contains("gamestore.php", ignoreCase = true) &&
                    !url.contains("freesnack", ignoreCase = true) &&
                    !url.contains("buysnack", ignoreCase = true)) -> {
                val matched = ARCADE_TICKET.find(html)?.groupValues?.getOrNull(1)
                    ?.replace(",", "")?.toIntOrNull()
                    ?: Regex("""([\d,]+)\s+Game Grid ticket""", RegexOption.IGNORE_CASE)
                        .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
                matched?.let {
                    prefs.setInt("availableGameGridTickets", it)
                    syncInventoryCount(inventory, GG_TICKET, it)
                }
                true
            }
            url.contains("gamestore.php", ignoreCase = true) &&
                (url.contains("freesnack", ignoreCase = true) ||
                    url.contains("action=buysnack", ignoreCase = true)) -> {
                parseIntPref(html, prefs, "availableSnackVouchers", SNACK_VOUCHER)
                if (html.contains("You acquire", ignoreCase = true)) {
                    prefs.setBoolean("_freeSnackPurchased", true)
                    prefs.setInt(
                        "_freeSnackPurchases",
                        prefs.getInt("_freeSnackPurchases", 0) + 1,
                    )
                }
                true
            }
            url.contains("whichshop=fdkol", ignoreCase = true) ||
                (url.contains("inv_use.php", ignoreCase = true) &&
                    url.contains("whichitem=5707", ignoreCase = true)) -> {
                parseIntPref(html, prefs, "availableFDKOLCommendations", FDKOL)
                prefs.setBoolean("_fdkolVisited", true)
                true
            }
            url.contains("whichshop=fantasyrealm", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableRubees", RUBEE)
                true
            }
            url.contains("whichshop=landfillstore", ignoreCase = true) ||
                url.contains("whichshop=dinseystore", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableFunFunds", FUN_FUNDS)
                true
            }
            url.contains("whichshop=walmart", ignoreCase = true) ||
                url.contains("whichshop=glaciest", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableWalmartGiftCertificates", WALMART)
                true
            }
            url.contains("whichshop=mrstore2002", ignoreCase = true) -> {
                Regex(
                    """You have ([\d,]+)\s+Mr\.?\s*Store 2002 Credit""",
                    RegexOption.IGNORE_CASE,
                ).find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
                    ?.let { prefs.setInt("availableMrStore2002Credits", it) }
                true
            }
            else -> matchInvRules(url, html, prefs, inventory)
        }
    }

    private fun matchInvRules(
        url: String,
        html: String,
        prefs: Preferences,
        inventory: InventoryManager?,
    ): Boolean {
        for (rule in INV_RULES) {
            if (rule.shops.none { shop ->
                    url.contains("whichshop=$shop", ignoreCase = true)
                }
            ) {
                continue
            }
            val raw = rule.pattern.find(html)?.groupValues?.getOrNull(1) ?: return true
            val count = if (rule.wordCount) {
                WORD_NUMBERS[raw.lowercase()]
                    ?: raw.replace(",", "").toIntOrNull()
                    ?: return true
            } else {
                raw.replace(",", "").toIntOrNull() ?: return true
            }
            rule.pref?.let { prefs.setInt(it, count) }
            syncInventoryCount(inventory, rule.itemId, count)
            return true
        }
        return false
    }

    private fun parseIntPref(html: String, prefs: Preferences, key: String, pattern: Regex) {
        pattern.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs.setInt(key, it) }
    }

    internal fun syncInventoryCount(inventory: InventoryManager?, itemId: Int, count: Int) {
        val inv = inventory ?: return
        val current = inv.getCount(itemId)
        val delta = count - current
        when {
            delta > 0 -> inv.gainItemLocally(itemId, delta)
            delta < 0 -> inv.consumeItemLocally(itemId, -delta)
        }
    }
}
