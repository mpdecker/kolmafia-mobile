package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.ash.AggregateType
import net.sourceforge.kolmafia.ash.AggregateValue
import net.sourceforge.kolmafia.ash.AshValue
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ZapGroupDatabase

/** Desktop RuntimeLibrary.get_related fold/zap decode helpers. */
object RelatedAggregate {

    fun decodeFoldToAggregate(
        itemName: String,
        itemIntType: AggregateType,
    ): AggregateValue {
        val group = FoldGroupDatabase.groupFor(itemName) ?: return AggregateValue(itemIntType)
        val result = AggregateValue(itemIntType)
        for (index in group.items.indices.reversed()) {
            val name = group.items[index]
            val key = ItemDatabase.getByName(name)?.name ?: name
            result[AshValue.item(key)] = AshValue.of((index + 1).toLong())
        }
        return result
    }

    fun decodeZapToAggregate(
        itemId: Int,
        itemIntType: AggregateType,
    ): AggregateValue {
        val group = ZapGroupDatabase.groupForItemId(itemId) ?: return AggregateValue(itemIntType)
        val result = AggregateValue(itemIntType)
        for (name in group.asReversed()) {
            val relatedId = ItemDatabase.getByName(name)?.id ?: continue
            if (relatedId == itemId) continue
            val key = ItemDatabase.getById(relatedId)?.name ?: name
            result[AshValue.item(key)] = AshValue.ZERO
        }
        return result
    }
}
