package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionInterchangeableIngredients
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.item.CreatableAmount

/**
 * ASH-P122 behavioral batch — craft introspection from concoctions database.
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
        if (!isPermitted(itemId)) return@regFn AshValue.ZERO
        val amount = CreatableAmount.quantityPossible(
            itemId,
            accessibleCount = { ingId, ingName ->
                kotlinx.coroutines.runBlocking { physicalAccessibleCount(ingId, ingName) }
            },
        )
        AshValue.of(amount.toLong())
    }

    regFn(scope, "creatable_amount", AshType.INT, listOf("id" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        if (!isPermitted(itemId)) return@regFn AshValue.ZERO
        val amount = CreatableAmount.quantityPossible(
            itemId,
            accessibleCount = { ingId, ingName ->
                kotlinx.coroutines.runBlocking { physicalAccessibleCount(ingId, ingName) }
            },
        )
        AshValue.of(amount.toLong())
    }
}
