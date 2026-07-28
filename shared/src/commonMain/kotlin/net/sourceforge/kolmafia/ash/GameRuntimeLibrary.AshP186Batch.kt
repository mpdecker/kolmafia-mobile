package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.inventory.PulverizeAggregate

/**
 * AshP186 — live ASH pulverize(item) v2 (direct mappings + bitmask decode).
 */
internal fun GameRuntimeLibrary.registerAshP186Batch(scope: AshScope) {
    val itemIntType = AggregateType(AshType.ITEM, AshType.INT)

    fun pulverizeItem(itemId: Int): AggregateValue {
        val pulver = EquipmentDatabase.getPulverization(itemId)
        return PulverizeAggregate.decodeToAggregate(pulver, itemIntType)
    }

    regFn(scope, "pulverize", itemIntType, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveAshItemId(args[0]) ?: return@regFn AggregateValue(itemIntType)
        pulverizeItem(itemId)
    }

    regFn(scope, "pulverize", itemIntType, listOf("id" to AshType.INT)) { _, args ->
        pulverizeItem(args[0].toLong().toInt())
    }
}
