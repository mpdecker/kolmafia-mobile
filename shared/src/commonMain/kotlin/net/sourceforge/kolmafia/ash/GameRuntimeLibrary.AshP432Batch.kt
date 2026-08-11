package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.campground.DwellingSync

/**
 * ASH-P432 behavioral batch — campground dwelling + inventory map accessors.
 */
internal fun GameRuntimeLibrary.registerAshP432Batch(scope: AshScope) {
    val itemIntType = AggregateType(AshType.ITEM, AshType.INT)

    regFn(scope, "get_dwelling", AshType.ITEM, emptyList()) { _, _ ->
        val itemId = DwellingSync.currentDwellingItemId(preferences)
        val name = gameDatabase?.item(itemId)?.name ?: "Item #$itemId"
        AshValue.item(name)
    }

    regFn(scope, "get_campground", itemIntType, emptyList()) { _, _ ->
        val result = AggregateValue(itemIntType)
        val state = character?.state?.value
        if (state?.inNuclearAutumn == true) {
            return@regFn result
        }
        val dwellingId = DwellingSync.currentDwellingItemId(preferences)
        val dwellingName = gameDatabase?.item(dwellingId)?.name ?: "Item #$dwellingId"
        result[AshValue.item(dwellingName)] = AshValue.of(1L)

        for ((itemId, qty) in CampgroundInventorySync.load(preferences)) {
            if (itemId == dwellingId) continue
            val name = gameDatabase?.item(itemId)?.name ?: "Item #$itemId"
            result[AshValue.item(name)] = AshValue.of(qty.toLong())
        }
        result
    }
}
