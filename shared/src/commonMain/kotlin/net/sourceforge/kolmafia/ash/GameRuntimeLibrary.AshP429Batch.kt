package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.campground.CampgroundAvailability
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.KitchenEquipmentGates

/**
 * ASH-P429 behavioral batch — campground workshed/kitchen/campground availability.
 */
internal fun GameRuntimeLibrary.registerAshP429Batch(scope: AshScope) {
    regFn(scope, "get_workshed", AshType.ITEM, emptyList()) { _, _ ->
        val itemId = CampgroundItemSync.currentWorkshedItemId(preferences)
        if (itemId < 0) {
            return@regFn AshValue.item("")
        }
        val name = gameDatabase?.item(itemId)?.name ?: "Item #$itemId"
        AshValue.item(name)
    }

    regFn(scope, "have_campground", AshType.BOOLEAN, emptyList()) { _, _ ->
        val state = character?.state?.value ?: return@regFn AshValue.of(false)
        AshValue.of(CampgroundAvailability.haveCampground(state))
    }

    regFn(scope, "have_chef", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(KitchenEquipmentGates.hasChef(preferences))
    }

    regFn(scope, "have_bartender", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(KitchenEquipmentGates.hasBartender(preferences))
    }
}
