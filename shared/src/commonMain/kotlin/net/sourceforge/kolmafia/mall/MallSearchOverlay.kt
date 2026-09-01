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
        val itemNames = resolveSearchItemNames(searchString)
        if (itemNames.isEmpty()) return mallRows.take(limit)

        val overlay = buildList {
            for (name in itemNames) {
                val itemId = ItemDatabase.getByName(name)?.id ?: continue
                addAll(npcListings(itemId))
                coinmasterListing(itemId)?.let(::add)
            }
        }
        val mallOnly = mallRows.filter { it.source == MallListingSource.MALL }
        return dedupe(overlay + mallOnly).take(limit)
    }

    /** Desktop finalizeList — fuzzy item names from search string. */
    fun finalizeList(searchString: String, rows: List<MallListing>): List<MallListing> {
        val names = resolveSearchItemNames(searchString)
        if (names.isEmpty()) return rows
        val overlay = buildList {
            for (name in names) {
                val itemId = ItemDatabase.getByName(name)?.id ?: continue
                addAll(npcListings(itemId))
                coinmasterListing(itemId)?.let(::add)
            }
        }
        return dedupe(overlay + rows)
    }

    internal fun resolveSearchItemNames(searchString: String): List<String> {
        val trimmed = searchString.trim()
        if (trimmed.isEmpty()) return emptyList()
        val matches = ItemDatabase.getMatchingNames(trimmed)
        if (matches.isNotEmpty()) return matches
        val exact = ItemDatabase.getByName(trimmed.trim('"'))?.name
        return if (exact != null) listOf(exact) else emptyList()
    }

    internal fun resolveSearchItemIds(searchString: String): Set<Int> =
        resolveSearchItemNames(searchString).mapNotNull { ItemDatabase.getByName(it)?.id }.toSet()

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
