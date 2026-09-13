package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.Crimbo23ShopSync

/**
 * Phases 6206–6220 — Crimbo seasonal thin-hub `parseResponse` batch (Behavioral Deepen XXXIX).
 *
 * Desktop coinmaster Crimbo*Request.parseResponse / CoinMasterRequest.parseBalance patterns
 * consolidated here so visit_url shop.php / crimbo*.php responses write token prefs.
 */
object CrimboHubResponseParse {

    private val SHOP_ID = Regex("""whichshop=([^&]+)""", RegexOption.IGNORE_CASE)
    private val CRIMBUX = Regex(
        """You currently have\s*<b>([\d,]+)</b>\s*Crimbux""",
        RegexOption.IGNORE_CASE,
    )
    private val CANDY_CREDIT = Regex(
        """You currently have.*?<b>([\d,]+)</b>\s*Candy Credit""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val CRIMBO_CREDIT = Regex(
        """(?:no|([\d,]+))\s*Crimbo Credit""",
        RegexOption.IGNORE_CASE,
    )
    private val CRIMBO_LUMPS = Regex(
        """(?:no|([\d,]+))\s*Crimbo Lump""",
        RegexOption.IGNORE_CASE,
    )
    private val SPIRIT_OF_HOLIDAY = Regex(
        """([\d,]+)\s*Spirit(?:s)? of (?:the )?Holiday""",
        RegexOption.IGNORE_CASE,
    )
    private val ELF_DRIVE = Regex(
        """([\d,]+)\s*Elf Drive""",
        RegexOption.IGNORE_CASE,
    )
    private val CRIMBO24_TOKEN = Regex(
        """([\d,]+)\s*(?:Crimbo|Holiday)\s*(?:Coin|Token|Credit)""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * @return true when a Crimbo URL was claimed (even if no token matched).
     */
    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        return when {
            url.contains("crimbo09.php", ignoreCase = true) ||
                url.contains("whichshop=crimbocartel", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableCrimbux", CRIMBUX)
                true
            }
            url.contains("crimbo11.php", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableCandyCredits", CANDY_CREDIT)
                true
            }
            url.contains("whichshop=crimbo14", ignoreCase = true) ||
                url.contains("crimbo14.php", ignoreCase = true) -> {
                parseOptionalCount(html, prefs, "availableCrimboCredits", CRIMBO_CREDIT)
                true
            }
            url.contains("whichshop=crimbo16", ignoreCase = true) ||
                url.contains("crimbo16.php", ignoreCase = true) -> {
                parseOptionalCount(html, prefs, "availableCrimboLumps", CRIMBO_LUMPS)
                true
            }
            url.contains("whichshop=crimbo17", ignoreCase = true) ||
                url.contains("crimbo17.php", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableSpiritOfHoliday", SPIRIT_OF_HOLIDAY)
                true
            }
            url.contains("whichshop=crimbo12", ignoreCase = true) -> {
                if (html.contains("You acquire", ignoreCase = true)) {
                    prefs.setBoolean("_crimbo12Crafted", true)
                }
                true
            }
            url.contains("whichshop=crimbo20booze", ignoreCase = true) ||
                url.contains("whichshop=crimbo20food", ignoreCase = true) ||
                url.contains("whichshop=crimbo20candy", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableElfDrive", ELF_DRIVE)
                true
            }
            url.contains("whichshop=crimbo23_", ignoreCase = true) -> {
                val shopId = SHOP_ID.find(url)?.groupValues?.getOrNull(1) ?: return true
                Crimbo23ShopSync.syncFromShopHtml(html, shopId, prefs)
                true
            }
            url.contains("whichshop=crimbo24_", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableCrimbo24Tokens", CRIMBO24_TOKEN)
                true
            }
            url.contains("whichshop=crimbo25_sammy", ignoreCase = true) -> {
                Regex("""([\d,]+)\s*Crymbocurrency""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
                    ?.let { prefs.setInt("availableCrymbocurrency", it) }
                true
            }
            // Desktop KringleRequest (crimbo19toys) — shop-row multi-cost; claim visit.
            url.contains("whichshop=crimbo19toys", ignoreCase = true) -> {
                prefs.setBoolean("_crimbo19ToysVisited", true)
                true
            }
            url.contains("whichshop=crimbo18", ignoreCase = true) ||
                url.contains("whichshop=crimbo19", ignoreCase = true) ||
                url.contains("whichshop=crimbo20cafe", ignoreCase = true) ||
                url.contains("whichshop=crimbo20blackmarket", ignoreCase = true) ||
                url.contains("whichshop=crimbo21", ignoreCase = true) ||
                url.contains("whichshop=crimbo25_cafe", ignoreCase = true) -> {
                prefs.setBoolean("_crimboCafeVisited", true)
                true
            }
            url.contains("crimbo_uncle.php", ignoreCase = true) ||
                url.contains("crimbo06.php", ignoreCase = true) ||
                url.contains("crimbo07.php", ignoreCase = true) ||
                url.contains("crimbo_factory.php", ignoreCase = true) -> {
                if (html.contains("You acquire", ignoreCase = true)) {
                    prefs.setBoolean("_crimboUncleCrafted", true)
                }
                true
            }
            url.contains("talktosocp", ignoreCase = true) -> {
                if (html.contains("Skeleton of Crimbo Past", ignoreCase = true) ||
                    html.contains("You acquire", ignoreCase = true)
                ) {
                    prefs.setBoolean("talkedToSkeletonOfCrimboPast", true)
                }
                true
            }
            else -> false
        }
    }

    private fun parseIntPref(html: String, prefs: Preferences, key: String, pattern: Regex) {
        pattern.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs.setInt(key, it) }
    }

    private fun parseOptionalCount(html: String, prefs: Preferences, key: String, pattern: Regex) {
        val m = pattern.find(html) ?: return
        val raw = m.groupValues.getOrNull(1).orEmpty()
        prefs.setInt(key, if (raw.isBlank()) 0 else raw.replace(",", "").toIntOrNull() ?: 0)
    }
}

/**
 * Phases 6221–6235 — craft / economy thin-hub `parseResponse` (Behavioral Deepen XXXIX).
 */
object CraftThinHubResponseParse {

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        var claimed = false
        if (GnomeTinkerRequestHub.registerRequest(url, null)) {
            if (html.contains("You acquire", ignoreCase = true) ||
                html.contains("tinker", ignoreCase = true)
            ) {
                prefs.setBoolean("_gnomeTinkerUsed", true)
            }
            claimed = true
        }
        if (PhineasRequestHub.registerRequest(url, null)) {
            if (html.contains("You acquire", ignoreCase = true)) {
                prefs.setBoolean("_phineasCrafted", true)
            }
            claimed = true
        }
        if (ChefStaffRequestHub.registerRequest(url, null)) {
            if (html.contains("You acquire", ignoreCase = true) ||
                html.contains("staff", ignoreCase = true)
            ) {
                prefs.setBoolean("_chefStaffCrafted", true)
            }
            claimed = true
        }
        if (TerminalExtrudeRequestHub.registerRequest(url, null)) {
            if (html.contains("You acquire", ignoreCase = true) ||
                html.contains("extrude", ignoreCase = true)
            ) {
                prefs.increment("_terminalExtrudes", 1)
            }
            claimed = true
        }
        if (SpacegateEquipmentRequestHub.registerRequest(url, null)) {
            if (html.contains("You acquire", ignoreCase = true)) {
                prefs.setBoolean("_spacegateEquipmentCrafted", true)
            }
            claimed = true
        }
        if (BurningLeavesRequest.registerRequest(url)) {
            if (html.contains("You acquire", ignoreCase = true) ||
                html.contains("leaf", ignoreCase = true)
            ) {
                prefs.setBoolean("_burningLeavesCrafted", true)
            }
            claimed = true
        }
        if (SausageOMaticRequest.registerRequest(url)) {
            if (html.contains("You acquire", ignoreCase = true) ||
                html.contains("sausage", ignoreCase = true)
            ) {
                prefs.increment("_sausageGrills", 1)
            }
            claimed = true
        }
        if (FantasyRealmRequest.registerRequest(url)) {
            Regex("""([\d,]+)\s*Rubee""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
                ?.let { prefs.setInt("availableRubees", it) }
            claimed = true
        }
        if (url.contains("sellstuff.php", ignoreCase = true) ||
            url.contains("sellstuff_ugly.php", ignoreCase = true)
        ) {
            AutoSellRequestHub.parseResponse(url, html, prefs)
            claimed = true
        }
        if (url.contains("freesnack", ignoreCase = true) ||
            (url.contains("gamestore.php", ignoreCase = true) &&
                url.contains("snack", ignoreCase = true))
        ) {
            if (html.contains("You acquire", ignoreCase = true)) {
                prefs.setBoolean("_freeSnackUsed", true)
            }
            claimed = true
        }
        return claimed
    }
}
