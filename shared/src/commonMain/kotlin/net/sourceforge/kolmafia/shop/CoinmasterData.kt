package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.session.SessionLogger

data class CoinmasterData(
    val masterName: String,
    val nickname: String,
    val nicknames: List<String> = emptyList(),
    val token: String?,
    val property: String? = null,
    val shopId: String?,
    val buyItems: List<ShopRow>,
    val sellItems: List<ShopRow>,
    val useItemField: Boolean = false,
    val buyUrl: String? = null,
    val buyAction: String = "buy",
    val sellUrl: String? = null,
    val sellAction: String = "sell",
    var isDisabled: Boolean = false,
    val visitShopRows: ((List<ShopRow>, Boolean, SessionLogger?) -> Unit)? = null,
    val visitShop: ((
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) -> Unit)? = null,
) {
    val allNicknames: List<String>
        get() = (listOf(nickname) + nicknames).distinct()

    fun buyRowFor(itemId: Int): ShopRow? =
        buyItems.firstOrNull { it.item.itemId == itemId && !it.isSkillPurchase }

    fun sellRowFor(itemId: Int): ShopRow? =
        sellItems.firstOrNull { it.item.itemId == itemId }

    /** Desktop [CoinmasterData.getShopRow] — item or skill buy row by id. */
    fun shopRowFor(thingId: Int): ShopRow? =
        buyItems.firstOrNull { it.item.itemId == thingId }
            ?: sellItems.firstOrNull { it.item.itemId == thingId }

    /**
     * Desktop [CoinmasterData.itemBuyPrice] — single-cost token price for an item row,
     * or legacy token price when the master is not shop-row style.
     */
    fun itemBuyPrice(itemId: Int): ItemStack? {
        if (!hasShopRowInventory()) {
            val row = buyRowFor(itemId) ?: return null
            val price = row.price
            if (price <= 0) return null
            val tokenId = tokenItemId() ?: return null
            return ItemStack(itemId = tokenId, count = price)
        }
        val row = buyItems.firstOrNull {
            !it.isSkillPurchase && it.item.itemId == itemId
        } ?: return null
        return row.costs.singleOrNull()
    }

    /**
     * Desktop [CoinmasterData.skillBuyPrice] — modern shop-row skill purchases only;
     * returns the sole cost stack when present.
     */
    fun skillBuyPrice(skillId: Int): ItemStack? {
        if (!hasShopRowInventory()) return null
        val row = buyItems.firstOrNull {
            it.isSkillPurchase && it.item.itemId == skillId
        } ?: return null
        return row.costs.singleOrNull()
    }

    /** Desktop [CoinmasterData.currencies] — token + shop-row buy costs (legacy buy uses token only). */
    fun currencyItemIds(): Set<Int> {
        val ids = mutableSetOf<Int>()
        token?.let { name ->
            ItemDatabase.getByName(name)?.id?.let { ids.add(it) }
        }
        if (isShopRowCoinmaster()) {
            for (row in buyItems) {
                for (cost in row.costs) {
                    if (!cost.isMeat) {
                        ids.add(cost.itemId)
                    }
                }
            }
        }
        return ids
    }

    /** Shop-row coinmaster (ROW lines with explicit cost stacks) vs legacy buy/sell lines. */
    fun isShopRowCoinmaster(): Boolean = buyItems.any { it.costs.isNotEmpty() }

    /** Desktop [CoinmasterData.getShopRows] != null — new-style coin row classification. */
    fun hasShopRowInventory(): Boolean = isShopRowCoinmaster()

    internal fun setDisabledForTest(disabled: Boolean) {
        isDisabled = disabled
    }

    fun isLegacyBuySellCoinmaster(): Boolean =
        sellItems.isNotEmpty() || buyItems.any { it.costs.isEmpty() && it.price > 0 }

    fun isAccessible(): Boolean = shopId != null || buyUrl != null || sellUrl != null

    fun inaccessibleReason(): String =
        if (isAccessible()) "" else "Shop not available"

    /** Desktop [CoinmasterData.getItem] — token currency item id when resolvable. */
    fun tokenItemId(): Int? = token?.let { ItemDatabase.getByName(it)?.id }

    /** Desktop [CoinmasterData.availableTokens] — inventory count, else pref, else 0. */
    fun availableTokens(preferences: Preferences?, inventory: Map<Int, Int>): Int {
        val itemId = tokenItemId()
        if (itemId != null) {
            if (itemId == HermitRequest.WORTHLESS_ITEM_ID) {
                return HermitRequest.worthlessCountFromMaps(inventory, emptyMap(), emptyMap())
            }
            return inventory[itemId] ?: 0
        }
        property?.let { pref ->
            return preferences?.getInt(pref, 0) ?: 0
        }
        return 0
    }
}
