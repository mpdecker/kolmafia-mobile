package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
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

    const val LEARNED_SHOPROWS_KEY = "learnedShopRows"

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
    private val logVisitShops = mutableSetOf<String>()
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
            val item = parseItemOrMeatOrSkill(parts[2]) ?: continue
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

    /** Desktop [ShopDatabase.registerShop] — returns true when shop id was previously unknown. */
    fun registerShop(
        shopId: String,
        shopName: String,
        shopType: ShopType = ShopType.NONE,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        val trimmed = shopName.trim()
        val key = shopId.lowercase()
        val isNew = !shopNames.containsKey(key)
        if (trimmed.isNotEmpty() && isNew) {
            shopNames[key] = trimmed
        }
        if (shopType != ShopType.NONE) {
            promoteShopType(shopId, shopType, sessionLogger)
        }
        return isNew && trimmed.isNotEmpty()
    }

    /** Desktop [ShopDatabase.registerShop] type promotion (NONE → COIN/NPC, NPC+COIN → NPCCOIN). */
    internal fun promoteShopType(
        shopId: String,
        shopType: ShopType,
        sessionLogger: SessionLogger? = null,
    ) {
        val key = shopId.lowercase()
        val existing = shopTypes[key] ?: ShopType.NONE
        if (shopType == ShopType.NONE || shopType == existing) return

        var resolved = shopType
        var changed = false
        val ok = existing != ShopType.CONC
        when (shopType) {
            ShopType.NPC -> {
                if (existing == ShopType.COIN) {
                    resolved = ShopType.NPCCOIN
                    changed = true
                } else if (existing == ShopType.NONE) {
                    changed = true
                }
            }
            ShopType.COIN -> {
                if (existing == ShopType.NPC) {
                    resolved = ShopType.NPCCOIN
                    changed = true
                } else if (existing == ShopType.NONE) {
                    changed = true
                }
            }
            else -> if (existing == ShopType.NONE) {
                changed = true
            }
        }
        if (!ok) {
            sessionLogger?.appendRawLine(
                "Shop id '$shopId' of type $shopType already registered as $existing",
            )
        }
        if (changed) {
            shopTypes[key] = resolved
        }
    }

    fun shopType(shopId: String): ShopType =
        shopTypes[shopId.lowercase()] ?: ShopType.NONE

    fun craftingType(shopId: String): String? =
        craftingTypes[shopId.lowercase()]

    /** Desktop [ShopDatabase.setLogVisits] — coinmaster shop.php visits log "Visiting …". */
    fun setLogVisits(shopId: String) {
        logVisitShops.add(shopId.lowercase())
    }

    fun logVisits(shopId: String): Boolean =
        logVisitShops.contains(shopId.lowercase())

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

    /** Restore visit-learned rows persisted in user prefs across sessions. */
    fun restoreLearnedRows(prefs: Preferences) {
        val raw = prefs.getString(LEARNED_SHOPROWS_KEY, "")
        if (raw.isBlank()) return
        for (line in raw.lines()) {
            parsePersistedLine(line)?.let { data ->
                if (byRowId.containsKey(data.rowId) || visitLearned.containsKey(data.rowId)) continue
                visitLearned[data.rowId] = data
            }
        }
        rebuildVisitInventoryFromLearned()
    }

    private fun rebuildVisitInventoryFromLearned() {
        val rowsByShop = visitLearned.values.groupBy { it.shopId.lowercase() }
        for ((shopId, rows) in rowsByShop) {
            CoinmasterVisitInventory.registerVisitBuyRows(
                shopId,
                rows.map { it.toShopRow() },
            )
        }
    }

    /** Append a newly visit-learned row to prefs (desktop shoprows.txt write-back substitute). */
    fun persistLearnedRow(prefs: Preferences, rowId: Int, shopId: String, row: ShopRow) {
        if (byRowId.containsKey(rowId)) return
        if (prefContainsRow(prefs, rowId)) return
        val line = toData(rowId, shopId, row)
        val existing = prefs.getString(LEARNED_SHOPROWS_KEY, "")
        prefs.setString(
            LEARNED_SHOPROWS_KEY,
            if (existing.isEmpty()) line else "$existing\n$line",
        )
    }

    fun toData(rowId: Int, shopId: String, row: ShopRow): String =
        ShopRowFormatting.toShopRowData(rowId, shopId, row)

    /** Desktop [ShopRowDatabase.parseItemOrMeatOrSkill]. */
    internal fun parseItemOrMeatOrSkill(token: String): ItemStack? {
        val trimmed = token.trim()
        SkillDefinitionDatabase.getByName(trimmed)?.let { skill ->
            return ItemStack(itemId = skill.id, count = 1, isSkill = true)
        }
        return CoinmasterDatabase.parseItemToken(token)
    }

    private fun parseCostToken(token: String): ItemStack? {
        val meatMatch = MEAT_PATTERN.find(token.trim()) ?: return CoinmasterDatabase.parseItemToken(token)
        val count = meatMatch.groupValues[1].replace(",", "").toIntOrNull() ?: return null
        return ItemStack(itemId = -1, count = count, isMeat = true)
    }

    private val MEAT_PATTERN = Regex("""([\d,]+)\s+Meat""", RegexOption.IGNORE_CASE)

    private fun prefContainsRow(prefs: Preferences, rowId: Int): Boolean {
        val prefix = "$rowId\t"
        return prefs.getString(LEARNED_SHOPROWS_KEY, "").lines().any { it.startsWith(prefix) }
    }

    private fun parsePersistedLine(line: String): ShopRowData? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null
        if (trimmed.toIntOrNull() != null) return null
        val parts = trimmed.split('\t')
        if (parts.size < 3) return null
        val rowId = parts[0].trim().toIntOrNull() ?: return null
        val shopId = parts[1].trim()
        val item = parseItemOrMeatOrSkill(parts[2]) ?: return null
        val costs = parts.drop(3).mapNotNull { parseCostToken(it) }
        if (costs.isEmpty()) return null
        return ShopRowData(rowId, shopId, item, costs)
    }

    internal fun resetForTest() {
        byRowId.clear()
        visitLearned.clear()
        shopNames.clear()
        shopTypes.clear()
        craftingTypes.clear()
        logVisitShops.clear()
        loaded = false
    }
}
