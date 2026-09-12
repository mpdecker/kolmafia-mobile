package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Phases 6331–6350 — misc shop/token `parseResponse` batch (Behavioral Deepen XLI).
 *
 * Covers register-only coinmaster leftovers whose desktop counterparts parse token
 * balances (arcade redemption tickets, free snack vouchers, FDKOL, Rubee, Beach Bucks,
 * FunFunds, Wal-Mart gift certificates) plus FreeSnack purchase accounting.
 */
object MiscShopTokenResponseParse {

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
    private val BEACH_BUCK = Regex(
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

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
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
                matched?.let { prefs.setInt("availableGameGridTickets", it) }
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
            url.contains("whichshop=sbb_brogurt", ignoreCase = true) ||
                url.contains("whichshop=brogurt", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableBeachBucks", BEACH_BUCK)
                true
            }
            url.contains("whichshop=landfillstore", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableFunFunds", FUN_FUNDS)
                true
            }
            url.contains("whichshop=walmart", ignoreCase = true) ||
                url.contains("whichshop=glaciest", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableWalmartGiftCertificates", WALMART)
                true
            }
            else -> false
        }
    }

    private fun parseIntPref(html: String, prefs: Preferences, key: String, pattern: Regex) {
        pattern.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs.setInt(key, it) }
    }
}
