package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Loads coinmaster shop data from bundled [coinmasters.txt] and [shops.txt].
 * ROW-style entries use shop.php; buy/sell rows with explicit ROW ids are included.
 */
@OptIn(ExperimentalResourceApi::class)
object CoinmasterDatabase {

    private val masters = mutableListOf<CoinmasterData>()
    private val byNickname = mutableMapOf<String, CoinmasterData>()
    private val byShopId = mutableMapOf<String, CoinmasterData>()
    private var loaded = false

    val all: List<CoinmasterData> get() = masters

    suspend fun load() {
        if (loaded) return
        ItemDatabase.load()
        StandardRewardDatabase.load()
        StandardRewardDatabase.derivePulverization()
        val shopsText = Res.readBytes("files/data/shops.txt").decodeToString()
        val coinText = Res.readBytes("files/data/coinmasters.txt").decodeToString()
        loadFromText(shopsText, coinText)
        ArmoryAndLeggeryShopRows.rebuild()
        loaded = true
    }

    internal fun loadFromText(shopsText: String, coinText: String, rebuildArmory: Boolean = true) {
        masters.clear()
        byNickname.clear()
        byShopId.clear()

        val shopNameToKey = parseShops(shopsText)
        val builders = mutableMapOf<String, Builder>()

        fun builderFor(masterName: String): Builder =
            builders.getOrPut(masterName) {
                Builder(masterName, shopNameToKey[masterName.lowercase()])
            }

        for (raw in coinText.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t')) continue

            val parts = line.split('\t')
            if (parts.size < 4) continue

            val masterName = parts[0].trim()
            val type = parts[1].trim()
            val builder = builderFor(masterName)

            when {
                type.startsWith("ROW") -> parseRowLine(parts, builder)
                type.equals("buy", ignoreCase = true) -> parseBuyLine(parts, builder)
                type.equals("sell", ignoreCase = true) -> parseSellLine(parts, builder)
            }
        }

        applyOverrides(builders)
        builders.values.map { it.build() }.forEach { register(it) }
        if (rebuildArmory && StandardRewardDatabase.allStandardRewards().isNotEmpty()) {
            ArmoryAndLeggeryShopRows.rebuild()
        }
    }

    internal fun registerOrReplaceArmory(buyRows: List<ShopRow>) {
        val shopId = ArmoryAndLeggeryShopRows.SHOP_ID
        masters.removeAll { it.shopId?.equals(shopId, ignoreCase = true) == true }
        byShopId.remove(shopId)
        val staleNicknames = byNickname.filterValues { it.shopId?.equals(shopId, ignoreCase = true) == true }.keys
        staleNicknames.forEach { byNickname.remove(it) }

        register(
            CoinmasterData(
                masterName = ArmoryAndLeggeryShopRows.MASTER_NAME,
                nickname = shopId,
                token = null,
                shopId = shopId,
                buyItems = buyRows,
                sellItems = emptyList(),
            ),
        )
    }

    fun findByNickname(nickname: String): CoinmasterData? =
        byNickname[nickname.lowercase()]

    fun findByMasterName(masterName: String): CoinmasterData? {
        val trimmed = masterName.trim()
        if (trimmed.isEmpty()) return null
        return masters.firstOrNull { it.masterName.equals(trimmed, ignoreCase = true) }
    }

    fun findByShopId(shopId: String): CoinmasterData? =
        byShopId[shopId.lowercase()]

    fun findBuyRowForItem(itemId: Int): Pair<CoinmasterData, ShopRow>? {
        for (master in masters) {
            val shopId = master.shopId?.lowercase()
            val swaggerVisited = master.nickname.equals("swagger", ignoreCase = true) &&
                CoinmasterVisitInventory.hasVisited(CoinmasterVisitInventory.SWAGGER)
            if (shopId != null && CoinmasterVisitInventory.hasVisited(shopId)) {
                CoinmasterVisitInventory.findBuyRow(shopId, itemId)?.let { return master to it }
                if (CoinmasterVisitInventory.isDynamicShop(shopId)) continue
            } else if (swaggerVisited) {
                CoinmasterVisitInventory.findBuyRow(CoinmasterVisitInventory.SWAGGER, itemId)
                    ?.let { return master to it }
            }
        }
        for (master in masters) {
            val shopId = master.shopId?.lowercase()
            if (shopId != null && CoinmasterVisitInventory.isDynamicShop(shopId) &&
                !CoinmasterVisitInventory.hasVisited(shopId)
            ) {
                continue
            }
            if (shopId != null && CoinmasterVisitInventory.hasVisited(shopId) &&
                CoinmasterVisitInventory.isDynamicShop(shopId)
            ) {
                continue
            }
            if (master.nickname.equals("swagger", ignoreCase = true) &&
                CoinmasterVisitInventory.hasVisited(CoinmasterVisitInventory.SWAGGER)
            ) {
                if (CoinmasterVisitInventory.findBuyRow(CoinmasterVisitInventory.SWAGGER, itemId) == null &&
                    !SWAGGER_SEASON_ITEM_IDS.contains(itemId)
                ) {
                    continue
                }
            }
            if (master.isDisabled) continue
            val row = master.buyRowFor(itemId) ?: continue
            return master to row
        }
        return null
    }

    fun findBuyRowForSkill(skillId: Int): Pair<CoinmasterData, ShopRow>? {
        for (master in masters) {
            val shopId = master.shopId?.lowercase() ?: continue
            if (!CoinmasterVisitInventory.hasVisited(shopId)) continue
            CoinmasterVisitInventory.findBuyRowBySkill(shopId, skillId)?.let { return master to it }
            if (CoinmasterVisitInventory.isDynamicShop(shopId)) continue
        }
        for (master in masters) {
            val shopId = master.shopId?.lowercase() ?: continue
            if (CoinmasterVisitInventory.isDynamicShop(shopId) &&
                !CoinmasterVisitInventory.hasVisited(shopId)
            ) {
                continue
            }
            if (CoinmasterVisitInventory.hasVisited(shopId) &&
                CoinmasterVisitInventory.isDynamicShop(shopId)
            ) {
                continue
            }
            if (master.isDisabled) continue
            master.buyItems.firstOrNull { row ->
                row.item.isSkill && row.item.itemId == skillId
            }?.let { return master to it }
        }
        return null
    }

    private val SWAGGER_SEASON_ITEM_IDS = setOf(
        7732, 4810, 4812, 8182, 8277, 8488, 8800, 9123, 9921, 10207, 10325, 10640, 11867, 4804,
    )

    /** Desktop CoinmasterData.availableSkillInternal probe (no public ASH). */
    fun containsBuySkill(
        skillId: Int,
        validate: Boolean = false,
        state: CharacterState = CharacterState(),
        prefs: Preferences? = null,
        hasSkill: (Int) -> Boolean = { false },
        hasEffect: (Int) -> Boolean = { false },
        accessibleCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (!validate) return findBuyRowForSkill(skillId) != null
        if (hasSkill(skillId)) return false
        val (master, row) = findBuyRowForSkill(skillId) ?: return false
        if (!CoinmasterAccessibility.isAccessible(master, state, prefs, accessibleCount, hasEffect)) {
            return false
        }
        if (!CoinmasterPurchaseAccessibility.visitInventorySkillAvailable(master, skillId)) {
            return false
        }
        return CoinmasterPurchaseProbe.affordableCount(row, state, accessibleCount) > 0
    }

    /** Desktop CoinmastersDatabase.contains(itemId, validate). */
    fun containsBuyItem(
        itemId: Int,
        validate: Boolean = false,
        state: CharacterState = CharacterState(),
        prefs: Preferences? = null,
        hasSkill: (Int) -> Boolean = { false },
        hasEffect: (Int) -> Boolean = { false },
        accessibleCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (!validate) return findBuyRowForItem(itemId) != null
        return CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
            itemId,
            state,
            prefs,
            hasSkill,
            hasEffect,
            accessibleCount,
        )
    }

    fun findSellRowForItem(itemId: Int): Pair<CoinmasterData, ShopRow>? {
        for (master in masters) {
            val shopId = master.shopId?.lowercase() ?: continue
            if (CoinmasterVisitInventory.hasVisitSellOverlay(shopId)) {
                CoinmasterVisitInventory.findSellRow(shopId, itemId)?.let { return master to it }
                continue
            }
            master.sellRowFor(itemId)?.let { return master to it }
        }
        return null
    }

    /** Desktop [CoinmasterData.canSellItem] with optional visit-overlay + inventory validate. */
    fun containsSellItem(
        itemId: Int,
        validate: Boolean = false,
        accessibleCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (!validate) return findSellRowForItem(itemId) != null
        val (master, row) = findSellRowForItem(itemId) ?: return false
        if (!CoinmasterPurchaseAccessibility.visitInventorySellAvailable(master, itemId)) {
            return false
        }
        return accessibleCount(itemId) >= 1
    }

    internal fun resetForTest() {
        masters.clear()
        byNickname.clear()
        byShopId.clear()
        loaded = false
        StandardRewardDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        ShopRowDatabase.resetForTest()
    }

    internal fun registerForTest(data: CoinmasterData) = register(data)

    private fun register(data: CoinmasterData) {
        val enriched = enrichWithVisitHooks(data)
        masters.add(enriched)
        byNickname[enriched.masterName.lowercase()] = enriched
        enriched.allNicknames.forEach { byNickname[it.lowercase()] = enriched }
        enriched.shopId?.let { shopId ->
            byShopId[shopId.lowercase()] = enriched
            if (shopId.isNotBlank() &&
                (enriched.buyUrl.isNullOrBlank() || enriched.buyUrl.startsWith("shop.php", ignoreCase = true))
            ) {
                ShopRowDatabase.setLogVisits(shopId)
            }
        }
    }

    private fun enrichWithVisitHooks(data: CoinmasterData): CoinmasterData {
        val shopId = data.shopId?.lowercase() ?: return data
        return when (shopId) {
            ArmoryAndLeggerySync.SHOP_ID -> data.copy(visitShopRows = ArmoryAndLeggerySync::applyVisitShopRows)
            FlowerTradeinSync.SHOP_ID -> data.copy(visitShopRows = FlowerTradeinSync::applyVisitShopRows)
            Crimbo25SammySync.SHOP_ID -> data.copy(visitShopRows = Crimbo25SammySync::applyVisitShopRows)
            MerchTableSync.SHOP_ID -> data.copy(
                visitShopRows = MerchTableSync::applyVisitShopRows,
                visitShop = MerchTableSync::applyVisitShop,
            )
            TrapperSync.SHOP_ID -> data.copy(visitShop = TrapperSync::applyVisitShop)
            DripArmoryPrefs.SHOP_ID -> data.copy(visitShop = DripArmoryPrefs::applyVisitShop)
            SeptEmberSync.SHOP_ID -> data.copy(visitShop = SeptEmberSync::applyVisitShop)
            SpinMasterLatheSync.SHOP_ID -> data.copy(visitShop = SpinMasterLatheSync::applyVisitShop)
            JunkMagazineSync.SHOP_ID -> data.copy(visitShop = JunkMagazineSync::applyVisitShop)
            BaconShopSync.SHOP_ID -> data.copy(visitShop = BaconShopSync::applyVisitShop)
            ArcadeShopSync.SHOP_ID -> data.copy(visitShop = ArcadeShopSync::applyVisitShop)
            KiwiShopSync.SHOP_ID -> data.copy(visitShop = KiwiShopSync::applyVisitShop)
            MysticShopSync.SHOP_ID -> data.copy(visitShop = MysticShopSync::applyVisitShop)
            ShoreShopSync.SHOP_ID -> data.copy(visitShop = ShoreShopSync::applyVisitShop)
            FiveDPrinterShopSync.SHOP_ID -> data.copy(visitShop = FiveDPrinterShopSync::applyVisitShop)
            ReplicaMrStoreSync.SHOP_ID -> data.copy(visitShop = ReplicaMrStoreSync::applyVisitShop)
            BlackMarketShopSync.SHOP_ID -> data.copy(visitShop = BlackMarketShopSync::applyVisitShop)
            PirateRealmShopSync.SHOP_ID -> data.copy(visitShop = PirateRealmShopSync::applyVisitShop)
            SwaggerShopSync.SHOP_ID -> data.copy(visitShop = SwaggerShopSync::applyVisitShop)
            else -> when {
                shopId in TimeTowerSync.CHRONER_SHOP_IDS && shopId != MerchTableSync.SHOP_ID ->
                    data.copy(visitShop = ChronerShopSync::applyVisitShop)
                shopId.startsWith("crimbo23_") ->
                    data.copy(visitShop = Crimbo23ShopSync::applyVisitShop)
                else -> data
            }
        }
    }

    private fun parseShops(text: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size < 2) continue
            val key = parts[0].trim()
            if (key.toIntOrNull() != null) continue
            val name = parts[1].trim()
            map[name.lowercase()] = key
        }
        return map
    }

    private fun parseRowLine(parts: List<String>, builder: Builder) {
        val type = parts[1].trim()
        if (!type.startsWith("ROW")) return
        val rowId = type.substring(3).toIntOrNull() ?: return
        val itemStack = parseItemToken(parts[2]) ?: return
        val costs = parts.drop(3).mapNotNull { parseItemToken(it) }
        builder.buyRows.add(
            ShopRow(rowId = rowId, item = itemStack, costs = costs)
        )
    }

    private fun parseBuyLine(parts: List<String>, builder: Builder) {
        val price = parts[2].trim().toIntOrNull() ?: return
        val itemStack = parseItemNameOnly(parts[3]) ?: return
        val rowId = extractRowId(parts.getOrNull(4)) ?: itemStack.itemId
        builder.buyRows.add(
            ShopRow(rowId = rowId, item = itemStack, costs = emptyList(), price = price)
        )
    }

    private fun parseSellLine(parts: List<String>, builder: Builder) {
        val price = parts[2].trim().toIntOrNull() ?: return
        val itemStack = parseItemNameOnly(parts[3]) ?: return
        val rowId = extractRowId(parts.getOrNull(4)) ?: itemStack.itemId
        builder.sellRows.add(
            ShopRow(rowId = rowId, item = itemStack, costs = emptyList(), price = price)
        )
    }

    private fun extractRowId(field: String?): Int? =
        field?.trim()?.takeIf { it.startsWith("ROW", ignoreCase = true) }
            ?.substring(3)?.toIntOrNull()

    private fun parseItemNameOnly(name: String): ItemStack? {
        val stack = parseItemToken(name) ?: return null
        return stack.copy(count = 1)
    }

    internal fun parseItemToken(token: String): ItemStack? {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return null

        val countMatch = Regex("""^(.+?)\s*\((\d+)\)$""").find(trimmed)
        val (rawName, count) = if (countMatch != null) {
            countMatch.groupValues[1].trim() to countMatch.groupValues[2].toInt()
        } else {
            trimmed to 1
        }

        if (rawName.equals("meat", ignoreCase = true)) {
            return ItemStack(itemId = -1, count = count, isMeat = true)
        }

        val decoded = decodeHtmlEntities(rawName)
        val item = ItemDatabase.getByName(decoded)
            ?: ItemDatabase.getByName(rawName)
            ?: return null
        return ItemStack(itemId = item.id, count = count)
    }

    private fun decodeHtmlEntities(text: String): String =
        text.replace(Regex("&([a-zA-Z]+);")) { match ->
            when (match.groupValues[1].lowercase()) {
                "eacute" -> "é"
                "egrave" -> "è"
                "aacute" -> "á"
                "ocirc" -> "ô"
                "uuml" -> "ü"
                "trade" -> "™"
                "quot" -> "\""
                "amp" -> "&"
                else -> match.value
            }
        }

    private fun applyOverrides(builders: MutableMap<String, Builder>) {
        SPECIAL_OVERRIDES.forEach { (name, override) ->
            val builder = builders.getOrPut(name) { Builder(name, override.shopId) }
            override.shopId?.let { builder.shopKey = it }
            override.nickname?.let { builder.nickname = it }
            override.aliases?.let { builder.extraNicknames.addAll(it) }
            override.token?.let { builder.token = it }
            override.property?.let { builder.property = it }
            override.useItemField?.let { builder.useItemField = it }
            override.buyUrl?.let { builder.buyUrl = it }
            override.sellUrl?.let { builder.sellUrl = it }
        }
        PROPERTY_OVERRIDES.forEach { (name, property) ->
            val builder = builders.getOrPut(name) { Builder(name, null) }
            builder.property = property
        }
    }

    private data class SpecialOverride(
        val nickname: String? = null,
        val aliases: List<String>? = null,
        val shopId: String? = null,
        val token: String? = null,
        val property: String? = null,
        val useItemField: Boolean? = null,
        val buyUrl: String? = null,
        val sellUrl: String? = null,
    )

    private val PROPERTY_OVERRIDES = mapOf(
        "Quartersmaster" to "availableQuarters",
        "Game Shoppe" to "availableStoreCredits",
        "PirateRealm Fun-a-Log" to "availableFunPoints",
        "Sept-Ember Censer" to "availableSeptEmbers",
        "Mr. Store 2002" to "availableMrStore2002Credits",
    )

    private val SPECIAL_OVERRIDES = mapOf(
        "Dimemaster" to SpecialOverride(
            nickname = "dimemaster",
            aliases = listOf("dmt"),
            token = "dime",
            property = "availableDimes",
            buyUrl = "bigisland.php",
            sellUrl = "bigisland.php",
        ),
        "Bounty Hunter Hunter" to SpecialOverride(
            nickname = "hunter",
            aliases = listOf("bhh"),
            token = "lucre",
            useItemField = true,
            buyUrl = "bounty.php",
            sellUrl = "bounty.php",
        ),
        "The Shore, Inc. Gift Shop" to SpecialOverride(
            nickname = "shore",
            shopId = "shore",
            token = "Shore Inc. Ship Trip Scrip",
        ),
        "The Crackpot Mystic's Shed" to SpecialOverride(
            nickname = "mystic",
            shopId = "mystic",
        ),
        "A Star Chart" to SpecialOverride(
            nickname = "starchart",
            shopId = "starchart",
        ),
        "The Swagger Shop" to SpecialOverride(
            nickname = "swagger",
            token = "swagger",
            property = "availableSwagger",
            buyUrl = "peevpee.php",
        ),
        "Vending Machine" to SpecialOverride(
            nickname = "vendingmachine",
            shopId = "damachine",
        ),
    )

    private class Builder(
        val masterName: String,
        var shopKey: String?,
    ) {
        var nickname: String = shopKey ?: masterName.lowercase().replace(Regex("[^a-z0-9]+"), "")
        val extraNicknames = mutableListOf<String>()
        var token: String? = null
        var property: String? = null
        var useItemField: Boolean = false
        var buyUrl: String? = null
        var sellUrl: String? = null
        val buyRows = mutableListOf<ShopRow>()
        val sellRows = mutableListOf<ShopRow>()

        fun build(): CoinmasterData {
            val dedupedBuy = buyRows.distinctBy { it.rowId to it.item.itemId }
            val dedupedSell = sellRows.distinctBy { it.rowId to it.item.itemId }
            return CoinmasterData(
                masterName = masterName,
                nickname = nickname,
                nicknames = extraNicknames,
                token = token,
                property = property,
                shopId = shopKey,
                buyItems = dedupedBuy,
                sellItems = dedupedSell,
                useItemField = useItemField,
                buyUrl = buyUrl,
                sellUrl = sellUrl ?: buyUrl,
            )
        }
    }
}
