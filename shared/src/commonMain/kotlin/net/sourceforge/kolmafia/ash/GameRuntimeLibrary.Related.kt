package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.EquipmentDatabase
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
        val itemName = args[0].toString()
        val itemId = net.sourceforge.kolmafia.data.ItemDatabase.getByName(itemName)?.id
            ?: gameDatabase?.item(itemName)?.id
            ?: return@regFn AggregateValue(itemIntType)
        when (args[1].toString().lowercase()) {
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
