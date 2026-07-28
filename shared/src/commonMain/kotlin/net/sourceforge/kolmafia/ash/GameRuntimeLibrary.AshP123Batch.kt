package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ConcoctionCreationCost
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

/**
 * ASH-P123 behavioral batch — shop probes and concoction pricing.
 */
internal fun GameRuntimeLibrary.registerAshP123Batch(scope: AshScope) {
    fun itemId(arg: AshValue): Int? = resolveAshItemId(arg)

    regFn(scope, "is_npc_item", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.FALSE
        AshValue.of(NpcStoreDatabase.containsItem(id, validate = false))
    }

    regFn(scope, "is_npc_item", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        AshValue.of(NpcStoreDatabase.containsItem(id, validate = false))
    }

    regFn(scope, "is_coinmaster_item", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.FALSE
        AshValue.of(CoinmasterDatabase.containsBuyItem(id, validate = false))
    }

    regFn(scope, "is_coinmaster_item", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        AshValue.of(CoinmasterDatabase.containsBuyItem(id, validate = false))
    }

    regFn(scope, "concoction_price", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.ZERO
        AshValue.of(concoctionPriceForItem(id))
    }
}

private fun GameRuntimeLibrary.concoctionPriceForItem(itemId: Int): Long {
    val itemName = ItemDatabase.getById(itemId)?.name ?: return 0L
    val concoction = ConcoctionDatabase.getByResult(itemName) ?: return 0L

    var cost = 0L
    for (ingredient in concoction.ingredients) {
        val ingId = ItemDatabase.getByName(ingredient.name)?.id ?: continue
        if (ingId < 0) continue
        val mallPrice = kotlinx.coroutines.runBlocking {
            mallManager?.cheapestPrice(ingredient.name) ?: -1L
        }
        val unitPrice = if (mallPrice < 0) Int.MAX_VALUE.toLong() else mallPrice
        cost += unitPrice * ingredient.quantity
    }
    cost += ConcoctionCreationCost.creationCost(concoction.methods)
    return minOf(cost, Int.MAX_VALUE.toLong())
}
