package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.PeeVPeeRequest
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [SwaggerShopRequest.visitShop] season overlay on peevpee.php?place=shop. */
object SwaggerShopSync {

    const val SHOP_ID = "swagger"

    private data class SwaggerSeason(
        val seasonName: String,
        val itemId: Int,
        val availablePref: String,
        val costPref: String,
        val swaggerPref: String,
    )

    private val SWAGGER_SEASONS = listOf(
        SwaggerSeason("pirate", 7732, "blackBartsBootyAvailable", "blackBartsBootyCost", "pirateSwagger"),
        SwaggerSeason("holiday", 4810, "holidayHalsBookAvailable", "holidayHalsBookCost", "holidaySwagger"),
        SwaggerSeason("ice", 4812, "antagonisticSnowmanKitAvailable", "antagonisticSnowmanKitCost", "iceSwagger"),
        SwaggerSeason("drunken", 8182, "mapToKokomoAvailable", "mapToKokomoCost", "drunkenSwagger"),
        SwaggerSeason("bear", 8277, "essenceOfBearAvailable", "essenceOfBearCost", "bearSwagger"),
        SwaggerSeason("numeric", 8488, "manualOfNumberologyAvailable", "manualOfNumberologyCost", "numericSwagger"),
        SwaggerSeason("optimal", 8800, "ROMOfOptimalityAvailable", "ROMOfOptimalityCost", "optimalSwagger"),
        SwaggerSeason("school", 9123, "schoolOfHardKnocksDiplomaAvailable", "schoolOfHardKnocksDiplomaCost", "schoolSwagger"),
        SwaggerSeason("safari", 9921, "guideToSafariAvailable", "guideToSafariCost", "safariSwagger"),
        SwaggerSeason("glitch", 10207, "glitchItemAvailable", "glitchItemCost", "glitchSwagger"),
        SwaggerSeason("average", 10325, "lawOfAveragesAvailable", "lawOfAveragesCost", "averageSwagger"),
        SwaggerSeason("Seasoning", 10640, "universalSeasoningAvailable", "universalSeasoningCost", "SeasoningSwagger"),
        SwaggerSeason("ironic", 11867, "bookOfIronyAvailable", "bookOfIronyCost", "ironicSwagger"),
        SwaggerSeason("none", 4804, "essenceOfAnnoyanceAvailable", "essenceOfAnnoyanceCost", ""),
    )

    private val SWAGGER_ITEM_PATTERN = Regex(
        """whichitem" value="(\d+)".*?\((\d[\d,]*) swagger\)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val SWAGGER_DESC_ITEM_PATTERN = Regex(
        """<tr><td><img.*?onclick='descitem\((.*?)\)'.*?<b>(?:<[^>]*>)?([^<]*).*?</b>.*?name="whichitem" value="(.*?)".*?\((.*?) swagger\).*?</td></tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val SWAGGER_SEASON_PATTERN = Regex(
        """You've earned -?([\d,]+) swagger during (?:a |an |)?([A-Za-z]+)(?: season)?""",
        RegexOption.IGNORE_CASE,
    )

    private val SWAGGER_BALANCE_PATTERN = Regex(
        """You have ([\d,]+) swagger""",
        RegexOption.IGNORE_CASE,
    )

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        if (url?.contains("action=buy", ignoreCase = true) == true) return

        val presentItems = mutableSetOf<Int>()
        val itemPrices = mutableMapOf<Int, Int>()
        for (match in SWAGGER_DESC_ITEM_PATTERN.findAll(html)) {
            val descId = match.groupValues[1]
            val itemName = match.groupValues[2].trim()
            val itemId = match.groupValues[3].toIntOrNull() ?: continue
            val price = match.groupValues[4].replace(",", "").toIntOrNull() ?: continue
            presentItems.add(itemId)
            itemPrices[itemId] = price
            val existing = ItemDatabase.getItemName(itemId)
            if (existing.isEmpty() || existing != itemName) {
                ItemDatabase.registerItem(itemId, itemName, descId)
            }
        }
        for (match in SWAGGER_ITEM_PATTERN.findAll(html)) {
            val itemId = match.groupValues[1].toIntOrNull() ?: continue
            val price = match.groupValues[2].replace(",", "").toIntOrNull() ?: continue
            presentItems.add(itemId)
            itemPrices[itemId] = price
        }

        for (season in SWAGGER_SEASONS) {
            prefs.setBoolean(season.availablePref, season.itemId in presentItems)
            itemPrices[season.itemId]?.let { prefs.setInt(season.costPref, it) }
        }

        parseBalance(html, prefs)
        parseSeason(html, prefs, sessionLogger)

        val visitRows = presentItems.map { itemId ->
            ShopRow(
                rowId = itemId,
                item = ItemStack(itemId = itemId, count = 1),
                price = itemPrices[itemId] ?: 0,
            )
        }
        CoinmasterVisitInventory.replaceBuyRows(CoinmasterVisitInventory.SWAGGER, visitRows)
    }

    fun applyBuy(
        html: String,
        url: String,
        prefs: Preferences?,
        inventoryManager: InventoryManager?,
        sessionLogger: SessionLogger?,
    ) {
        if (prefs == null) return
        if (!url.contains("action=buy", ignoreCase = true)) return
        if (html.contains("You don't have enough") || html.contains("Huh?")) return

        val itemId = PeeVPeeRequest.queryField(url, "whichitem")?.toIntOrNull() ?: return
        val count = PeeVPeeRequest.queryField(url, "howmany")?.toIntOrNull()?.takeIf { it > 0 } ?: 1
        inventoryManager?.gainItemLocally(itemId, count)
        if (parseBalance(html, prefs) == null) {
            val cost = lookupPrice(itemId, prefs) * count
            if (cost > 0) {
                val current = prefs.getInt("availableSwagger", 0)
                prefs.setInt("availableSwagger", (current - cost).coerceAtLeast(0))
            }
        }
        ConcoctionDatabase.markRefreshNeeded()
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger?): Boolean {
        if (!url.contains("peevpee.php", ignoreCase = true)) return false
        if (!url.contains("place=shop", ignoreCase = true) &&
            !url.contains("action=buy", ignoreCase = true)
        ) {
            return false
        }
        val action = PeeVPeeRequest.queryField(url, "action")
        if (action == null) {
            sessionLogger?.appendRawLine("Visiting The Swagger Shop")
            return true
        }
        if (!action.equals("buy", ignoreCase = true)) return false
        val itemId = PeeVPeeRequest.queryField(url, "whichitem")?.toIntOrNull()
        if (itemId == null) return true
        val count = PeeVPeeRequest.queryField(url, "howmany")?.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val cost = lookupPrice(itemId, prefs = null) * count
        val itemName = ItemDatabase.getItemName(itemId).ifEmpty { "item #$itemId" }
        sessionLogger?.appendRawLine("trading $cost swagger for $count $itemName")
        return true
    }

    private fun parseSeason(html: String, prefs: Preferences, sessionLogger: SessionLogger?) {
        val match = SWAGGER_SEASON_PATTERN.find(html) ?: return
        val swagger = match.groupValues[1].replace(",", "").toIntOrNull() ?: 0
        val seasonName = match.groupValues[2]
        prefs.setString("currentPVPSeason", seasonName)
        val season = SWAGGER_SEASONS.firstOrNull {
            it.seasonName.equals(seasonName, ignoreCase = true)
        }
        if (season == null) {
            val message = "*** Unknown PVP season: $seasonName"
            sessionLogger?.appendRawLine(message)
            return
        }
        if (season.swaggerPref.isNotEmpty()) {
            prefs.setInt(season.swaggerPref, swagger)
        }
    }

    private fun parseBalance(html: String, prefs: Preferences): Int? {
        val match = SWAGGER_BALANCE_PATTERN.find(html) ?: return null
        val swagger = match.groupValues[1].replace(",", "").toIntOrNull() ?: return null
        prefs.setInt("availableSwagger", swagger)
        return swagger
    }

    private fun lookupPrice(itemId: Int, prefs: Preferences?): Int {
        CoinmasterVisitInventory.findBuyRow(SHOP_ID, itemId)?.price?.takeIf { it > 0 }?.let { return it }
        CoinmasterRegistry.findByNickname(SHOP_ID)?.buyRowFor(itemId)?.price?.takeIf { it > 0 }?.let { return it }
        if (prefs != null) {
            SWAGGER_SEASONS.firstOrNull { it.itemId == itemId }?.let { season ->
                prefs.getInt(season.costPref, 0).takeIf { it > 0 }?.let { return it }
            }
        }
        return 0
    }
}
