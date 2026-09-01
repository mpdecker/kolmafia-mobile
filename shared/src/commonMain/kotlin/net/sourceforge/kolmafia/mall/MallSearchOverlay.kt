package net.sourceforge.kolmafia.mall

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.shop.CoinmasterRegistry

/**
 * Desktop MallSearchRequest.addNPCStoreItem / addCoinMasterItem / finalizeList —
 * prepend NPC and coinmaster purchase rows to mall search results.
 */
object MallSearchOverlay {
    const val NPC_SHOP_ID = 0
    const val COINMASTER_SHOP_ID = -1

    fun merge(
        searchString: String,
        mallRows: List<MallListing>,
        limit: Int = Int.MAX_VALUE,
    ): List<MallListing> {
        val itemIds = linkedSetOf<Int>()
        mallRows.forEach { if (it.itemId > 0) itemIds += it.itemId }
        itemIds += resolveSearchItemIds(searchString)
        if (itemIds.isEmpty()) return mallRows.take(limit)

        val overlay = buildList {
            for (itemId in itemIds) {
                addAll(npcListings(itemId))
                coinmasterListing(itemId)?.let(::add)
            }
        }
        val mallOnly = mallRows.filter { it.source == MallListingSource.MALL }
        return dedupe(overlay + mallOnly).take(limit)
    }

    internal fun resolveSearchItemIds(searchString: String): Set<Int> {
        val trimmed = searchString.trim().trim('"')
        if (trimmed.isEmpty()) return emptySet()
        val exact = ItemDatabase.getByName(trimmed)?.id ?: return emptySet()
        return if (exact > 0) setOf(exact) else emptySet()
    }

    internal fun npcListings(itemId: Int): List<MallListing> {
        val entry = NpcStoreDatabase.itemEntry(itemId) ?: return emptyList()
        val (store, item) = entry
        return listOf(
            MallListing(
                shopId = NPC_SHOP_ID,
                shopName = store.storeName,
                itemId = itemId,
                price = item.price.toLong(),
                quantity = 1,
                limit = 1,
                canPurchase = true,
                source = MallListingSource.NPC,
            ),
        )
    }

    internal fun coinmasterListing(itemId: Int): MallListing? {
        val (master, row) = CoinmasterRegistry.findBuyRowForItem(itemId) ?: return null
        if (row.price <= 0) return null
        return MallListing(
            shopId = COINMASTER_SHOP_ID,
            shopName = master.masterName,
            itemId = itemId,
            price = row.price.toLong(),
            quantity = 1,
            limit = 1,
            canPurchase = true,
            source = MallListingSource.COINMASTER,
        )
    }

    private fun dedupe(rows: List<MallListing>): List<MallListing> {
        val seen = linkedSetOf<String>()
        return rows.filter { row ->
            val key = "${row.source}:${row.itemId}:${row.shopId}:${row.shopName}:${row.price}"
            seen.add(key)
        }
    }
}
