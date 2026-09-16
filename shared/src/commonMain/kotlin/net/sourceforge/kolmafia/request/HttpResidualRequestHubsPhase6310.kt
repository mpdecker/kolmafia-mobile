package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Phases 6266–6285 — non-Crimbo legacy coinmaster `parseResponse` batch (Behavioral Deepen XL).
 *
 * Mirrors desktop [CoinMasterRequest.parseBalance] / ResponseTextParser routes for
 * mrstore.php, monkeycastle.php (Big Brother), Fudge Wand choice 562, Isotope Smithery,
 * AWOL Quartermaster, Dedigitizer visit, Swagger peevpee shop deepen.
 * LV: lunar isotope inventory fan-in.
 */
object LegacyCoinmasterResponseParse {

    private val MR_A = Regex(
        """You have (\w+) Mr\. Accessor(?:y|ies) to trade""",
        RegexOption.IGNORE_CASE,
    )
    private val SAND_DOLLAR = Regex(
        """(?:You've.*?got|You.*? have) (?:<b>)?([\d,]+)(?:</b>)? sand dollar""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val FUDGECULE = Regex(
        """(?:You've.*?got|You.*? have) (?:<b>)?([\d,]+)(?:</b>)? fudgecule""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val LUNAR_ISOTOPE = Regex(
        """You have ([\d,]+) lunar isotopes?""",
        RegexOption.IGNORE_CASE,
    )
    private val AWOL_COMMENDATION = Regex(
        """(?:You've.*?got|You.*? have) (?:<b>)?([\d,]+)(?:</b>)? A\.\s*W\.\s*O\.\s*L\. commendation""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val SWAGGER = Regex(
        """([\d,]+)\s+Swagger""",
        RegexOption.IGNORE_CASE,
    )

    private val WORD_NUMBERS = mapOf(
        "no" to 0, "a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3,
        "four" to 4, "five" to 5, "six" to 6, "seven" to 7, "eight" to 8,
        "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
    )

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
    ): Boolean {
        val prefs = preferences ?: return false
        return when {
            url.contains("mrstore.php", ignoreCase = true) -> {
                parseMrStore(url, html, prefs)
                true
            }
            url.contains("monkeycastle.php", ignoreCase = true) &&
                (url.contains("who=2", ignoreCase = true) ||
                    url.contains("action=buyitem", ignoreCase = true) ||
                    html.contains("sand dollar", ignoreCase = true)) -> {
                parseSandDollars(html, prefs)
                if (html.contains("Big Brother", ignoreCase = true) ||
                    url.contains("who=2", ignoreCase = true)
                ) {
                    prefs.setBoolean("bigBrotherRescued", true)
                }
                true
            }
            (url.contains("whichchoice=562", ignoreCase = true) ||
                (url.contains("whichitem=5441", ignoreCase = true) &&
                    url.contains("inv_use.php", ignoreCase = true))) -> {
                parseIntPref(html, prefs, "availableFudgecules", FUDGECULE)
                if (html.contains("You acquire", ignoreCase = true)) {
                    prefs.setBoolean("_fudgeWandUsed", true)
                }
                true
            }
            url.contains("whichshop=isotope", ignoreCase = true) ||
                url.contains("whichshop=elvishp1", ignoreCase = true) ||
                url.contains("whichshop=elvishp2", ignoreCase = true) ||
                url.contains("whichshop=elvishp3", ignoreCase = true) -> {
                LUNAR_ISOTOPE.find(html)?.groupValues?.getOrNull(1)
                    ?.replace(",", "")?.toIntOrNull()?.let {
                        prefs.setInt("availableLunarIsotopes", it)
                        MiscShopTokenResponseParse.syncInventoryCount(
                            inventory,
                            MiscShopTokenResponseParse.LUNAR_ISOTOPE,
                            it,
                        )
                    }
                true
            }
            url.contains("whichshop=awol", ignoreCase = true) ||
                (url.contains("inv_use.php", ignoreCase = true) &&
                    url.contains("whichitem=5116", ignoreCase = true)) -> {
                parseIntPref(html, prefs, "availableAWOLCommendations", AWOL_COMMENDATION)
                true
            }
            url.contains("whichshop=cyber_dedigitizer", ignoreCase = true) ||
                url.contains("whichshop=dedigitizer", ignoreCase = true) -> {
                prefs.setBoolean("_dedigitizerVisited", true)
                if (html.contains("You acquire", ignoreCase = true)) {
                    prefs.setBoolean("_dedigitizerPurchase", true)
                }
                true
            }
            url.contains("peevpee.php", ignoreCase = true) &&
                url.contains("place=shop", ignoreCase = true) -> {
                parseIntPref(html, prefs, "availableSwagger", SWAGGER)
                true
            }
            url.contains("whichshop=batman_cave", ignoreCase = true) ||
                url.contains("whichshop=batman", ignoreCase = true) -> {
                Regex("""([\d,]+)\s+Bat[- ]?(?:buck|coin)""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
                    ?.let { prefs.setInt("availableBatbucks", it) }
                true
            }
            url.contains("whichshop=cgold", ignoreCase = true) ||
                url.contains("whichshop=infernodisco", ignoreCase = true) ||
                url.contains("discogift", ignoreCase = true) -> {
                prefs.setBoolean("_discoGiftCoVisited", true)
                true
            }
            url.contains("friars.php", ignoreCase = true) -> {
                if (html.contains("blessing", ignoreCase = true) ||
                    html.contains("You acquire an effect", ignoreCase = true) ||
                    url.contains("action=", ignoreCase = true)
                ) {
                    prefs.setBoolean("_friarsBlessingUsed", true)
                }
                true
            }
            else -> false
        }
    }

    private fun parseMrStore(url: String, html: String, prefs: Preferences) {
        MR_A.find(html)?.groupValues?.getOrNull(1)?.let { raw ->
            prefs.setInt("availableMrAccessories", parseCountWord(raw))
        }
        when {
            url.contains("action=a_to_b", ignoreCase = true) &&
                html.contains("You acquire", ignoreCase = true) ->
                prefs.setBoolean("_mrStoreExchangedAtoB", true)
            url.contains("action=b_to_a", ignoreCase = true) &&
                html.contains("You acquire", ignoreCase = true) ->
                prefs.setBoolean("_mrStoreExchangedBtoA", true)
            url.contains("action=pullmras", ignoreCase = true) &&
                html.contains("You acquire", ignoreCase = true) ->
                prefs.setBoolean("_mrStorePulledMrA", true)
            url.contains("action=pullunclebs", ignoreCase = true) &&
                html.contains("You acquire", ignoreCase = true) ->
                prefs.setBoolean("_mrStorePulledUncleB", true)
        }
        if (html.contains("onClick='javascript:descitem", ignoreCase = true)) {
            prefs.setBoolean("_mrStoreInventorySeen", true)
        }
    }

    private fun parseSandDollars(html: String, prefs: Preferences) {
        parseIntPref(html, prefs, "availableSandDollars", SAND_DOLLAR)
        if (html.contains("haven't got any sand dollars", ignoreCase = true)) {
            prefs.setInt("availableSandDollars", 0)
        }
    }

    private fun parseIntPref(html: String, prefs: Preferences, key: String, pattern: Regex) {
        pattern.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let { prefs.setInt(key, it) }
    }

    private fun parseCountWord(raw: String): Int {
        val trimmed = raw.trim().lowercase()
        trimmed.toIntOrNull()?.let { return it }
        return WORD_NUMBERS[trimmed] ?: 0
    }
}
