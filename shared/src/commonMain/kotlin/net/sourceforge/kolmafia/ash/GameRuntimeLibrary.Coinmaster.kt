package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterManager

internal fun GameRuntimeLibrary.registerCoinmasterFunctions(scope: AshScope) {

    fun resolveMaster(value: AshValue): CoinmasterData? =
        coinmasterManager?.resolveMaster(value.toString())

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    regFn(scope, "is_accessible", AshType.BOOLEAN, listOf("master" to AshType.COINMASTER)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.FALSE
        AshValue.of(coinmasterManager?.isAccessible(master) ?: master.isAccessible())
    }

    regFn(scope, "inaccessible_reason", AshType.STRING, listOf("master" to AshType.COINMASTER)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.EMPTY_STRING
        val reason = coinmasterManager?.inaccessibleReason(master)
            ?: master.inaccessibleReason()
        AshValue.of(reason)
    }

    regFn(scope, "visit", AshType.BOOLEAN, listOf("master" to AshType.COINMASTER)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking { coinmasterManager?.visit(master) ?: false }
        AshValue.of(ok)
    }

    // Desktop coinmaster buy inventory-delta success; XLIV batch coalesce mirrors sell
    regFn(scope, "buy", AshType.BOOLEAN,
        listOf("master" to AshType.COINMASTER, "count" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.FALSE
        val count = args[1].toLong().toInt()
        val itemId = resolveItemId(args[2].toString()) ?: return@regFn AshValue.FALSE
        if (count <= 0) return@regFn AshValue.TRUE
        if (isBatching(rt)) {
            val nick = master.nickname.ifBlank { master.shopId ?: master.masterName }
            batchCommand(rt, "coinmaster", "buy $nick", pilcrowItemParams(count, itemId))
            return@regFn AshValue.TRUE
        }
        val ok = kotlinx.coroutines.runBlocking {
            val initial = inventoryManager?.getCount(itemId) ?: 0
            val bought = coinmasterManager?.buy(master, itemId, count) ?: 0
            val after = inventoryManager?.getCount(itemId) ?: (initial + bought)
            // Desktop: initial + count == after
            if (inventoryManager != null) after == initial + count
            else bought >= count
        }
        AshValue.of(ok)
    }

    // Desktop sell(coinmaster, count, item) → BOOLEAN continueValue (+ batch coalesce)
    regFn(scope, "sell", AshType.BOOLEAN,
        listOf("master" to AshType.COINMASTER, "count" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.FALSE
        val count = args[1].toLong().toInt()
        val itemId = resolveItemId(args[2].toString()) ?: return@regFn AshValue.FALSE
        if (count <= 0) return@regFn AshValue.TRUE
        if (isBatching(rt)) {
            val nick = master.nickname.ifBlank { master.shopId ?: master.masterName }
            batchCommand(rt, "coinmaster", "sell $nick", pilcrowItemParams(count, itemId))
            return@regFn AshValue.TRUE
        }
        val sold = kotlinx.coroutines.runBlocking {
            coinmasterManager?.sell(master, itemId, count) ?: 0
        }
        AshValue.of(sold >= count)
    }

    regFn(scope, "buys_item", AshType.BOOLEAN,
        listOf("master" to AshType.COINMASTER, "it" to AshType.ITEM)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.FALSE
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.FALSE
        AshValue.of(coinmasterManager?.buysItem(master, itemId) ?: false)
    }

    regFn(scope, "buy_price", AshType.INT,
        listOf("master" to AshType.COINMASTER, "it" to AshType.ITEM)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.ZERO
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.ZERO
        AshValue.of((coinmasterManager?.buyPrice(master, itemId) ?: 0).toLong())
    }

    regFn(scope, "sells_item", AshType.BOOLEAN,
        listOf("master" to AshType.COINMASTER, "it" to AshType.ITEM)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.FALSE
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.FALSE
        AshValue.of(coinmasterManager?.sellsItem(master, itemId) ?: false)
    }

    regFn(scope, "sell_price", AshType.INT,
        listOf("master" to AshType.COINMASTER, "it" to AshType.ITEM)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.ZERO
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.ZERO
        AshValue.of((coinmasterManager?.sellPrice(master, itemId) ?: 0).toLong())
    }

    // Desktop sell_price(coinmaster, skill) — token cost of skill purchase row
    regFn(scope, "sell_price", AshType.INT,
        listOf("master" to AshType.COINMASTER, "skill" to AshType.SKILL)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AshValue.ZERO
        val skillId = gameDatabase?.skill(args[1].toString())?.id
            ?: args[1].toString().toIntOrNull()
            ?: return@regFn AshValue.ZERO
        val row = master.buyItems.firstOrNull { it.isSkillPurchase && it.item.itemId == skillId }
            ?: return@regFn AshValue.ZERO
        val price = when {
            row.price > 0 -> row.price
            row.costs.isNotEmpty() -> row.costs.first().count
            else -> 0
        }
        AshValue.of(price.toLong())
    }

    // Phase 4489: sell_cost returns item→int cost map (desktop ITEM_TO_INT), item + skill overloads.
    val sellCostType = AggregateType(AshType.ITEM, AshType.INT)
    fun sellCostMap(master: CoinmasterData, thingId: Int, asSkill: Boolean): AggregateValue {
        val result = AggregateValue(sellCostType)
        val row = if (asSkill) {
            master.buyItems.firstOrNull { it.isSkillPurchase && it.item.itemId == thingId }
        } else {
            master.buyRowFor(thingId) ?: master.sellRowFor(thingId)
        }
        if (row != null && row.costs.isNotEmpty()) {
            for (cost in row.costs) {
                if (cost.isMeat) continue
                val name = ItemDatabase.getById(cost.itemId)?.name ?: "Item #${cost.itemId}"
                result[AshValue.item(name)] = AshValue.of(cost.count.toLong())
            }
            return result
        }
        // Legacy single-token sell price for items.
        if (!asSkill) {
            val price = coinmasterManager?.sellPrice(master, thingId) ?: 0
            if (price > 0) {
                val tokenName = master.token ?: return result
                result[AshValue.item(tokenName)] = AshValue.of(price.toLong())
            }
        }
        return result
    }
    regFn(scope, "sell_cost", sellCostType,
        listOf("master" to AshType.COINMASTER, "it" to AshType.ITEM)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AggregateValue(sellCostType)
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AggregateValue(sellCostType)
        sellCostMap(master, itemId, asSkill = false)
    }
    regFn(scope, "sell_cost", sellCostType,
        listOf("master" to AshType.COINMASTER, "skill" to AshType.SKILL)) { _, args ->
        val master = resolveMaster(args[0]) ?: return@regFn AggregateValue(sellCostType)
        val skillId = gameDatabase?.skill(args[1].toString())?.id
            ?: args[1].toString().toIntOrNull()
            ?: return@regFn AggregateValue(sellCostType)
        sellCostMap(master, skillId, asSkill = true)
    }
}

internal fun GameRuntimeLibrary.registerCraftFunctions(scope: AshScope) {

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    regFn(scope, "craft", AshType.INT,
        listOf("mode" to AshType.STRING, "count" to AshType.INT, "item1" to AshType.ITEM, "item2" to AshType.ITEM)) { _, args ->
        val mode = args[0].toString()
        val count = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.ZERO
        val id1 = resolveItemId(args[2].toString()) ?: return@regFn AshValue.ZERO
        val id2 = resolveItemId(args[3].toString()) ?: return@regFn AshValue.ZERO
        val created = kotlinx.coroutines.runBlocking {
            craftRequest?.craft(mode, count, id1, id2) ?: 0
        }
        AshValue.of(created.toLong())
    }

    regFn(scope, "create", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.FALSE
        AshValue.of(kotlinx.coroutines.runBlocking { createItem(itemId, 1) })
    }

    regFn(scope, "create", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val count = args[0].toLong().toInt()
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.FALSE
        if (count <= 0) return@regFn AshValue.TRUE
        AshValue.of(kotlinx.coroutines.runBlocking { createItem(itemId, count) })
    }

    regFn(scope, "create", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "count" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.FALSE
        val count = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        AshValue.of(kotlinx.coroutines.runBlocking { createItem(itemId, count) })
    }
}
