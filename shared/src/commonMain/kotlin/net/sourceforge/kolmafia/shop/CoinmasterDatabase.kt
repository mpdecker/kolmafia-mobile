package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase
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
        val shopsText = Res.readBytes("files/data/shops.txt").decodeToString()
        val coinText = Res.readBytes("files/data/coinmasters.txt").decodeToString()
        loadFromText(shopsText, coinText)
        loaded = true
    }

    internal fun loadFromText(shopsText: String, coinText: String) {
        masters.clear()
        byNickname.clear()
        byShopId.clear()

        val shopNameToKey = parseShops(shopsText)
        val builders = mutableMapOf<String, Builder>()

        fun builderFor(masterName: String): Builder =
            builders.getOrPut(masterName) { Builder(masterName, shopNameToKey[masterName]) }

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
            val row = master.buyRowFor(itemId) ?: continue
            return master to row
        }
        return null
    }

    internal fun resetForTest() {
        masters.clear()
        byNickname.clear()
        byShopId.clear()
        loaded = false
    }

    private fun register(data: CoinmasterData) {
        masters.add(data)
        byNickname[data.masterName.lowercase()] = data
        data.allNicknames.forEach { byNickname[it.lowercase()] = data }
        data.shopId?.let { byShopId[it.lowercase()] = data }
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
            override.useItemField?.let { builder.useItemField = it }
            override.buyUrl?.let { builder.buyUrl = it }
            override.sellUrl?.let { builder.sellUrl = it }
        }
    }

    private data class SpecialOverride(
        val nickname: String? = null,
        val aliases: List<String>? = null,
        val shopId: String? = null,
        val token: String? = null,
        val useItemField: Boolean? = null,
        val buyUrl: String? = null,
        val sellUrl: String? = null,
    )

    private val SPECIAL_OVERRIDES = mapOf(
        "Dimemaster" to SpecialOverride(
            nickname = "dimemaster",
            aliases = listOf("dmt"),
            token = "dime",
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
    )

    private class Builder(
        val masterName: String,
        var shopKey: String?,
    ) {
        var nickname: String = shopKey ?: masterName.lowercase().replace(Regex("[^a-z0-9]+"), "")
        val extraNicknames = mutableListOf<String>()
        var token: String? = null
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
