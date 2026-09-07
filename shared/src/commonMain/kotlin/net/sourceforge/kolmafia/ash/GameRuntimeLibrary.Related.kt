package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.PulverizeAggregate
import net.sourceforge.kolmafia.inventory.RelatedAggregate

internal fun GameRuntimeLibrary.registerRelatedFunctions(scope: AshScope) {
    val itemIntType = AggregateType(AshType.ITEM, AshType.INT)
    regFn(
        scope,
        "get_related",
        AshType.AGGREGATE,
        listOf("item" to AshType.ITEM, "type" to AshType.STRING),
    ) { _, args ->
        val itemRef = args[0]
        val itemId = resolveRelatedItemId(itemRef) ?: return@regFn AggregateValue(itemIntType)
        val itemName = ItemDatabase.getById(itemId)?.name
            ?: gameDatabase?.item(itemId)?.name
            ?: itemRef.toString()
        when (args[1].toString().trim().lowercase()) {
            "pulverize" -> {
                val pulver = EquipmentDatabase.getPulverization(itemId)
                PulverizeAggregate.decodeToAggregate(pulver, itemIntType)
            }
            "fold" -> RelatedAggregate.decodeFoldToAggregate(itemName, itemIntType)
            "zap" -> RelatedAggregate.decodeZapToAggregate(itemId, itemIntType)
            else -> AggregateValue(itemIntType)
        }
    }
}

private fun GameRuntimeLibrary.resolveRelatedItemId(itemRef: AshValue): Int? {
    resolveAshItemId(itemRef)?.takeIf { it > 0 }?.let { return it }
    val content = itemRef.content
    when (content) {
        is Long -> if (content > 0) return content.toInt()
        is Int -> if (content > 0) return content
        is String -> content.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
    }
    val name = itemRef.toString()
    name.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
    return ItemDatabase.getByName(name)?.id ?: gameDatabase?.item(name)?.id
}
