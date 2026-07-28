package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Desktop [ShopDatabase.SHOP] — shop type from bundled shops.txt. */
enum class ShopType {
    NONE,
    CONC,
    NPC,
    COIN,
    NPCCOIN,
    ;

    companion object {
        fun parse(typeName: String): ShopType =
            entries.firstOrNull { it.name.equals(typeName.trim(), ignoreCase = true) } ?: NONE
    }
}

/** Desktop [ShopRowDatabase] — bundled shoprows.txt row-id lookup + runtime visit-learned rows. */
@OptIn(ExperimentalResourceApi::class)
object ShopRowDatabase {

    data class ShopRowData(
        val rowId: Int,
        val shopId: String,
        val item: ItemStack,
        val costs: List<ItemStack>,
    ) {
        fun toShopRow(): ShopRow = ShopRow(rowId = rowId, item = item, costs = costs)
    }

    private val byRowId = mutableMapOf<Int, ShopRowData>()
    private val visitLearned = mutableMapOf<Int, ShopRowData>()
    private val shopNames = mutableMapOf<String, String>()
    private val shopTypes = mutableMapOf<String, ShopType>()
    private val craftingTypes = mutableMapOf<String, String>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        ItemDatabase.load()
        val shopRowsText = Res.readBytes("files/data/shoprows.txt").decodeToString()
        val shopsText = Res.readBytes("files/data/shops.txt").decodeToString()
        loadFromText(shopRowsText, shopsText)
        loaded = true
    }

    internal fun loadFromText(shopRowsText: String, shopsText: String = "") {
        byRowId.clear()
        visitLearned.clear()
        if (shopsText.isNotBlank()) {
            loadShopNames(shopsText)
        }
        for (raw in shopRowsText.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 3) continue
            val rowId = parts[0].trim().toIntOrNull() ?: continue
            val shopId = parts[1].trim()
            val item = CoinmasterDatabase.parseItemToken(parts[2]) ?: continue
            val costs = parts.drop(3).mapNotNull { parseCostToken(it) }
            if (costs.isEmpty()) continue
            if (byRowId.containsKey(rowId)) continue
            byRowId[rowId] = ShopRowData(rowId, shopId, item, costs)
        }
    }

    private fun loadShopNames(shopsText: String) {
        shopNames.clear()
        shopTypes.clear()
        craftingTypes.clear()
        for (raw in shopsText.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size < 2) continue
            val shopId = parts[0].trim()
            if (shopId.toIntOrNull() != null) continue
            val key = shopId.lowercase()
            shopNames[key] = parts[1].trim()
            if (parts.size >= 3) {
                val shopType = ShopType.parse(parts[2])
                shopTypes[key] = shopType
                if (shopType == ShopType.CONC && parts.size >= 4) {
                    craftingTypes[key] = parts[3].trim()
                }
            }
        }
    }

    fun shopName(shopId: String): String =
        shopNames[shopId.lowercase()] ?: shopId

    fun shopType(shopId: String): ShopType =
        shopTypes[shopId.lowercase()] ?: ShopType.NONE

    fun craftingType(shopId: String): String? =
        craftingTypes[shopId.lowercase()]

    fun getShopRow(rowId: Int): ShopRow? =
        visitLearned[rowId]?.toShopRow() ?: byRowId[rowId]?.toShopRow()

    fun getShopRowData(rowId: Int): ShopRowData? =
        visitLearned[rowId] ?: byRowId[rowId]

    fun isKnownRow(rowId: Int): Boolean =
        byRowId.containsKey(rowId) || visitLearned.containsKey(rowId)

    fun registerVisitRow(rowId: Int, shopId: String, row: ShopRow) {
        visitLearned[rowId] = ShopRowData(
            rowId = rowId,
            shopId = shopId,
            item = row.item,
            costs = row.costs,
        )
    }

    fun toData(rowId: Int, shopId: String, row: ShopRow): String =
        ShopRowFormatting.toShopRowData(rowId, shopId, row)

    private fun parseCostToken(token: String): ItemStack? {
        val meatMatch = MEAT_PATTERN.find(token.trim()) ?: return CoinmasterDatabase.parseItemToken(token)
        val count = meatMatch.groupValues[1].replace(",", "").toIntOrNull() ?: return null
        return ItemStack(itemId = -1, count = count, isMeat = true)
    }

    private val MEAT_PATTERN = Regex("""([\d,]+)\s+Meat""", RegexOption.IGNORE_CASE)

    internal fun resetForTest() {
        byRowId.clear()
        visitLearned.clear()
        shopNames.clear()
        shopTypes.clear()
        craftingTypes.clear()
        loaded = false
    }
}
