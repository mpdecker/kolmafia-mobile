package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.CafeAccessibility
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.request.CafeDailySpecialSync
import net.sourceforge.kolmafia.shop.CoinmasterRegistry

/**
 * AshP985–990 Track Q — Shop / mall residuals.
 *
 * Phase 985: have_shop, have_display
 * Phase 986: mall_prices
 * Phase 987: get_shop_log
 * Phase 988: put_shop_using_storage / well_stocked
 * Phase 989: daily_special
 * Phase 990: sells_skill (coinmaster skillBuyPrice)
 * Phases 6691–6710: skill cost / cafe / well_stocked residual deepen
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
                when (val c = item.content) {
                    is Long -> c.toInt().takeIf { it > 0 }
                    is Int -> c.takeIf { it > 0 }
                    else -> null
                }
                    ?: item.toString().toIntOrNull()?.takeIf { it > 0 }
                    ?: gameDatabase?.item(item.toString())?.id
                    ?: ItemDatabase.getByName(item.toString())?.id
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
        ensureSoldItemsRetrieved()
        val entries = runBlocking { manageStoreRequest?.getStoreLog()?.getOrNull() }.orEmpty()
        entries.forEachIndexed { index, entry -> result[AshValue.of(index.toLong())] = AshValue.of(entry) }
        result
    }

    // ── Phase 988: put_shop_using_storage / well_stocked ────────────
    regFn(scope, "put_shop_using_storage", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = gameDatabase?.item(args[2].toString())?.id ?: return@regFn AshValue.FALSE
        val price = args[0].toLong().toInt()
        val limit = args[1].toLong().toInt()
        val available = runBlocking { storageRequest?.fetchContents()?.get(itemId) ?: 0 }
        if (available <= 0) return@regFn AshValue.TRUE
        if (isBatching(rt)) {
            batchCommand(
                rt, "shop", "put using storage",
                "${pilcrowItemParams(available, itemId)} @ $price limit $limit",
            )
            return@regFn AshValue.TRUE
        }
        ensureSoldItemsRetrieved()
        val ok = runBlocking {
            manageStoreRequest?.addItem(
                itemId, price, limit, available, fromStorage = true,
            )?.isSuccess == true
        }
        AshValue.of(ok)
    }

    regFn(scope, "put_shop_using_storage", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "qty" to AshType.INT,
            "it" to AshType.ITEM)) { rt, args ->
        val itemId = gameDatabase?.item(args[3].toString())?.id ?: return@regFn AshValue.FALSE
        val qty = args[2].toLong().toInt()
        if (qty <= 0) return@regFn AshValue.TRUE
        val price = args[0].toLong().toInt()
        val limit = args[1].toLong().toInt()
        if (isBatching(rt)) {
            batchCommand(
                rt, "shop", "put using storage",
                "${pilcrowItemParams(qty, itemId)} @ $price limit $limit",
            )
            return@regFn AshValue.TRUE
        }
        ensureSoldItemsRetrieved()
        val available = runBlocking { storageRequest?.fetchContents()?.get(itemId) ?: 0 }
        val ok = qty in 1..available && runBlocking {
            manageStoreRequest?.addItem(
                itemId, price, limit, qty, fromStorage = true,
            )?.isSuccess == true
        }
        AshValue.of(ok)
    }

    // Desktop well_stocked — mall purchase listings only (no NPC/coinmaster)
    regFn(scope, "well_stocked", AshType.BOOLEAN,
        listOf("itemName" to AshType.STRING, "quantity" to AshType.INT, "price" to AshType.INT)) { _, args ->
        val itemId = gameDatabase?.item(args[0].toString())?.id
            ?: ItemDatabase.getByName(args[0].toString())?.id
            ?: return@regFn AshValue.FALSE
        if (itemId < 1) return@regFn AshValue.FALSE
        val quantity = args[1].toLong().toInt()
        val price = args[2].toLong()
        val autosell = ItemDatabase.getById(itemId)?.autosellPrice?.toLong() ?: 0L
        if (quantity < 6 || price < 2L * autosell) return@regFn AshValue.FALSE
        val manager = mallManager ?: return@regFn AshValue.FALSE
        val listings = runBlocking { manager.searchListings(args[0].toString(), 20) }
        var available = 0
        for (listing in listings.sortedBy { it.price }) {
            // Desktop: only MallPurchaseRequest rows
            if (listing.source != net.sourceforge.kolmafia.mall.MallListingSource.MALL) continue
            if (listing.shopId <= 0 || !listing.canPurchase) continue
            if (!net.sourceforge.kolmafia.mall.MallPurchaseRequest.canPurchase(listing.shopId)) continue
            if (listing.price > price) {
                return@regFn AshValue.of(available >= quantity)
            }
            // limit <= 0 means unrestricted store stock for this listing
            val canGet = if (listing.limit <= 0) listing.quantity
            else minOf(listing.quantity, listing.limit)
            available += canGet
            if (available >= quantity) return@regFn AshValue.TRUE
        }
        AshValue.of(available >= quantity)
    }

    // Desktop daily_special — gnomads MicroBrewery else Canadia ChezSnootée; live cafe visit sync
    regFn(scope, "daily_special", AshType.ITEM, emptyList()) { _, _ ->
        val state = character?.state?.value
        val prefs = preferences
        val useMicro = CafeAccessibility.isMicroBreweryAvailable(state, prefs)
        val useChez = CafeAccessibility.isChezSnooteeAvailable(state)
        if (!useMicro && !useChez) return@regFn AshValue.item("none")
        // Desktop: gnomads takes priority when available
        var specialId = CafeDailySpecialSync.currentSpecialItemId(prefs)
        var special = CafeDailySpecialSync.currentSpecialName(prefs)
        if (special.isNullOrBlank() && specialId == null && cafeRequest != null) {
            runBlocking {
                if (useMicro) cafeRequest.visitMenu("2", prefs)
                else if (useChez) cafeRequest.visitMenu("1", prefs)
            }
            specialId = CafeDailySpecialSync.currentSpecialItemId(prefs)
            special = CafeDailySpecialSync.currentSpecialName(prefs)
        }
        // Prefer item-id resolution (desktop AdventureResult item id)
        if (specialId != null && specialId > 0) {
            val byId = gameDatabase?.item(specialId)?.name
                ?: ItemDatabase.getById(specialId)?.name
            if (!byId.isNullOrBlank()) return@regFn AshValue.item(byId)
        }
        val specialName = special?.takeIf { it.isNotBlank() } ?: return@regFn AshValue.item("none")
        val resolved = gameDatabase?.item(specialName)?.name
            ?: ItemDatabase.getByName(specialName)?.name
            ?: specialName
        AshValue.item(resolved)
    }

    // ── Phase 990 / 6691: sells_skill via desktop skillBuyPrice ──────
    regFn(scope, "sells_skill", AshType.BOOLEAN,
        listOf("cm" to AshType.COINMASTER, "skill" to AshType.SKILL)) { _, args ->
        val master = coinmasterManager?.resolveMaster(args[0].toString())
            ?: CoinmasterRegistry.findByNickname(args[0].toString())
            ?: return@regFn AshValue.FALSE
        val skillId = gameDatabase?.skill(args[1].toString())?.id
            ?: args[1].toString().toIntOrNull()
            ?: return@regFn AshValue.FALSE
        // Desktop: skillBuyPrice(skillId) != null (single-cost shop-row only)
        AshValue.of(master.skillBuyPrice(skillId) != null)
    }
}
