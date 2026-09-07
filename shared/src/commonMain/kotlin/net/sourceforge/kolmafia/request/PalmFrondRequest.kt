package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.inventory.InventoryManager

/** Desktop [net.sourceforge.kolmafia.request.PalmFrondRequest] multi-use weave. */
object PalmFrondRequest {
    const val WEAVING_MANUAL = 2654

    fun hasManual(inventory: InventoryManager?): Boolean =
        (inventory?.getCount(WEAVING_MANUAL) ?: 0) > 0

    fun registerRequest(url: String): Boolean =
        MultiUseRequest.registerRequest(url) &&
            (url.contains("whichitem=${ItemPool.PALM_FROND}") ||
                url.contains("whichitem=${ItemPool.PALM_FROND_FAN}"))
}
