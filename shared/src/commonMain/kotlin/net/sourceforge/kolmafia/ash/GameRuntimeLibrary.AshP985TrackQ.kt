package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.shop.CoinmasterRegistry
import net.sourceforge.kolmafia.session.StoreManager

/**
 * AshP985–990 Track Q — Shop / mall residuals.
 *
 * Phase 985: have_shop, have_display
 * Phase 986: mall_prices
 * Phase 987: get_shop_log
 * Phase 988: put_shop_using_storage / well_stocked
 * Phase 989: daily_special
 * Phase 990: sells_skill (coinmaster placeholder)
 */
internal fun GameRuntimeLibrary.registerAshP985TrackQBatch(scope: AshScope) {
    // ── Phase 985: have_shop / have_display ──────────────────────────
    regFn(scope, "have_shop", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.hasStore ?: false)
    }

    regFn(scope, "have_display", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.hasDisplayCase ?: false)
    }

    // ── Phase 986: mall_prices ──────────────────────────────────────
    regFn(scope, "mall_prices", AshType.INT,
        listOf("category" to AshType.STRING)) { _, args ->
        val count = runBlocking { mallManager?.mallPrices(args[0].toString())?.size ?: 0 }
        AshValue.of(count.toLong())
    }

    val itemSet = AggregateType(AshType.ITEM, AshType.BOOLEAN)
    regFn(scope, "mall_prices", AshType.INT,
        listOf("items" to itemSet)) { _, args ->
        val ids = (args[0] as? AggregateValue)?.map?.keys
            ?.mapNotNull { item ->
                item.toString().toIntOrNull()
                    ?: gameDatabase?.item(item.toString())?.id
            }.orEmpty()
        val count = runBlocking { mallManager?.refreshMallPrices(ids) ?: 0 }
        AshValue.of(count.toLong())
    }

    regFn(scope, "mall_prices", AshType.INT,
        listOf("category" to AshType.STRING, "tiers" to AshType.STRING)) { _, args ->
        val count = runBlocking { mallManager?.mallPrices(args[0].toString(), args[1].toString())?.size ?: 0 }
        AshValue.of(count.toLong())
    }

    // ── Phase 987: get_shop_log ─────────────────────────────────────
    val stringArray = AggregateType(AshType.INT, AshType.STRING)
    regFn(scope, "get_shop_log", stringArray, emptyList()) { _, _ ->
        val result = AggregateValue(stringArray)
        val entries = runBlocking { manageStoreRequest?.getStoreLog()?.getOrNull() }.orEmpty()
        entries.forEachIndexed { index, entry -> result[AshValue.of(index.toLong())] = AshValue.of(entry) }
        result
    }

    // ── Phase 988: put_shop_using_storage / well_stocked ────────────
    regFn(scope, "put_shop_using_storage", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = gameDatabase?.item(args[2].toString())?.id ?: return@regFn AshValue.FALSE
        val available = runBlocking { storageRequest?.fetchContents()?.get(itemId) ?: 0 }
        val ok = available > 0 && runBlocking {
            manageStoreRequest?.addItem(
                itemId, args[0].toLong().toInt(), args[1].toLong().toInt(), available, fromStorage = true,
            )?.isSuccess == true
        }
        AshValue.of(ok)
    }

    regFn(scope, "put_shop_using_storage", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "qty" to AshType.INT,
            "it" to AshType.ITEM)) { _, args ->
        val itemId = gameDatabase?.item(args[3].toString())?.id ?: return@regFn AshValue.FALSE
        val qty = args[2].toLong().toInt()
        val available = runBlocking { storageRequest?.fetchContents()?.get(itemId) ?: 0 }
        val ok = qty in 1..available && runBlocking {
            manageStoreRequest?.addItem(
                itemId, args[0].toLong().toInt(), args[1].toLong().toInt(), qty, fromStorage = true,
            )?.isSuccess == true
        }
        AshValue.of(ok)
    }

    regFn(scope, "well_stocked", AshType.BOOLEAN,
        listOf("itemName" to AshType.STRING, "quantity" to AshType.INT, "price" to AshType.INT)) { _, args ->
        val itemId = gameDatabase?.item(args[0].toString())?.id ?: return@regFn AshValue.FALSE
        val quantity = args[1].toLong().toInt()
        val price = args[2].toLong()
        val autosell = ItemDatabase.getById(itemId)?.autosellPrice?.toLong() ?: 0L
        if (quantity < 6 || price < 2L * autosell) return@regFn AshValue.FALSE
        val manager = mallManager
        if (manager == null) {
            return@regFn AshValue.of(
                StoreManager.shopAmount(itemId) >= quantity &&
                    StoreManager.getPrice(itemId) <= price,
            )
        }
        val listings = runBlocking { manager.searchListings(args[0].toString(), 20) }
        var available = 0
        for (listing in listings.sortedBy { it.price }) {
            if (!listing.canPurchase || listing.price > price) break
            available += minOf(listing.quantity, listing.limit.coerceAtLeast(0))
            if (available >= quantity) return@regFn AshValue.TRUE
        }
        AshValue.FALSE
    }

    // ── Phase 989: daily_special ────────────────────────────────────
    regFn(scope, "daily_special", AshType.ITEM, emptyList()) { _, _ ->
        val special = preferences?.getString("dailySpecial", "")?.takeIf { it.isNotBlank() }
        AshValue.item(special ?: "")
    }

    // ── Phase 990: sells_skill ─────────────────────────────────────
    regFn(scope, "sells_skill", AshType.BOOLEAN,
        listOf("cm" to AshType.COINMASTER)) { _, _ ->
        AshValue.FALSE
    }

    regFn(scope, "sells_skill", AshType.BOOLEAN,
        listOf("cm" to AshType.COINMASTER, "skill" to AshType.SKILL)) { _, args ->
        val master = CoinmasterRegistry.findByNickname(args[0].toString())
        val skillId = gameDatabase?.skill(args[1].toString())?.id
            ?: args[1].toString().toIntOrNull()
            ?: return@regFn AshValue.FALSE
        AshValue.of(master?.buyItems?.any { it.isSkillPurchase && it.item.itemId == skillId } == true)
    }
}
