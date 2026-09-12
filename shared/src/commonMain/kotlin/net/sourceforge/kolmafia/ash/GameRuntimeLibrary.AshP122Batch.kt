package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionInterchangeableIngredients
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.item.CreatableAmount

/**
 * ASH-P122 behavioral batch — craft introspection from concoctions database.
 * Phases 6531–6550 Track C: creatable_amount prefers runtime quantityPossible /
 * visibleTotal; get_ingredients refreshes dirty concoction cache.
 */
internal fun GameRuntimeLibrary.registerAshP122Batch(scope: AshScope) {
    val itemIntType = AggregateType(AshType.ITEM, AshType.INT)

    fun state() = character?.state?.value ?: CharacterState()
    fun skills() = skillManager?.state?.value?.skills ?: emptyList()

    fun isPermitted(itemId: Int): Boolean {
        val itemName = ItemDatabase.getById(itemId)?.name ?: return false
        val concoction = ConcoctionDatabase.getByResult(itemName) ?: return false
        return ConcoctionPermitted.isPermittedMethod(
            concoction,
            state(),
            skills(),
            accessibleCount = { ingId ->
                val name = ItemDatabase.getById(ingId)?.name ?: return@isPermittedMethod 0
                kotlinx.coroutines.runBlocking { physicalAccessibleCount(ingId, name) }
            },
            prefs = preferences,
            familiarUsable = { familiarId -> craftFamiliarUsable(familiarId) },
        )
    }

    fun ingredientsForItemId(itemId: Int): AggregateValue {
        ConcoctionDatabase.ensureRefreshed()
        val result = AggregateValue(itemIntType)
        val itemName = ItemDatabase.getById(itemId)?.name ?: return result
        val concoction = ConcoctionDatabase.getByResult(itemName) ?: return result
        if (!isPermitted(itemId)) return result
        val availableCountById: (Int) -> Int = { ingId ->
            val name = ItemDatabase.getById(ingId)?.name
            if (name == null) 0
            else kotlinx.coroutines.runBlocking { physicalAccessibleCount(ingId, name) }
        }
        val priceFor = { ingId: Int ->
            val mallPrice = mallPriceManager?.getHistoricalPrice(ingId)?.toInt() ?: 0
            if (mallPrice > 0) mallPrice else ConcoctionInterchangeableIngredients.defaultPriceFor(ingId)
        }
        val ingredients = ConcoctionInterchangeableIngredients.resolve(
            concoction,
            itemId,
            availableCountById,
            priceFor,
        )
        for (ingredient in ingredients) {
            val ingId = ItemDatabase.getByName(ingredient.name)?.id ?: continue
            if (ingId < 0) continue
            val key = itemAshValue(ingId)
            val existing = result[key]?.toLong()?.toInt() ?: 0
            result[key] = AshValue.of((existing + ingredient.quantity).toLong())
        }
        return result
    }

    fun creatableAmountFor(itemId: Int): Long {
        ConcoctionDatabase.ensureRefreshed()
        val itemName = ItemDatabase.getById(itemId)?.name ?: return 0L
        // Desktop CreateItemRequest.getQuantityPossible → post-refresh creatable snapshot.
        if (ConcoctionDatabase.getRuntime(itemName) != null) {
            return ConcoctionDatabase.quantityPossible(itemName).toLong()
        }
        if (!isPermitted(itemId)) return 0L
        return CreatableAmount.quantityPossible(
            itemId,
            accessibleCount = { ingId, ingName ->
                kotlinx.coroutines.runBlocking { physicalAccessibleCount(ingId, ingName) }
            },
            preferRuntime = true,
        ).toLong()
    }

    regFn(scope, "get_ingredients", itemIntType, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveAshItemId(args[0]) ?: return@regFn AggregateValue(itemIntType)
        ingredientsForItemId(itemId)
    }

    regFn(scope, "get_ingredients", itemIntType, listOf("id" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        ingredientsForItemId(itemId)
    }

    regFn(scope, "creatable_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveAshItemId(args[0]) ?: return@regFn AshValue.ZERO
        AshValue.of(creatableAmountFor(itemId))
    }

    regFn(scope, "creatable_amount", AshType.INT, listOf("id" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        AshValue.of(creatableAmountFor(itemId))
    }
}
