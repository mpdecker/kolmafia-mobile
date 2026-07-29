package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
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
        SwaggerSeason("none", 4804, "essenceOfAnnoyanceAvailable", "essenceOfAnnoyanceCost", "availableSwagger"),
    )

    private val SWAGGER_ITEM_PATTERN = Regex(
        """whichitem" value="(\d+)".*?\((\d[\d,]*) swagger\)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val SWAGGER_SEASON_PATTERN = Regex(
        """You've earned -?([\d,]+) swagger during (?:a |an |)?(pirate|holiday|ice|drunken|bear|numeric|optimal|school|safari|glitch|average|Seasoning|ironic|none)(?: season)?""",
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

        SWAGGER_SEASON_PATTERN.find(html)?.let { match ->
            val swagger = match.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            val seasonName = match.groupValues[2]
            prefs.setString("currentPVPSeason", seasonName)
            SWAGGER_SEASONS.firstOrNull {
                it.seasonName.equals(seasonName, ignoreCase = true)
            }?.let { season ->
                prefs.setInt(season.swaggerPref, swagger)
            }
        }

        val visitRows = presentItems.map { itemId ->
            ShopRow(
                rowId = itemId,
                item = ItemStack(itemId = itemId, count = 1),
                price = itemPrices[itemId] ?: 0,
            )
        }
        CoinmasterVisitInventory.replaceBuyRows(CoinmasterVisitInventory.SWAGGER, visitRows)
    }
}
