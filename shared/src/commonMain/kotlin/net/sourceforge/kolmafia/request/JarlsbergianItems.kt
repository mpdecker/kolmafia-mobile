package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.shop.CoinmasterDatabase

/** Desktop [JarlsbergRequest.isJarlsbergian] — Cosmic Kitchen buyables + mediocre lager. */
object JarlsbergianItems {
    const val MEDIOCRE_LAGER = 6215

    fun isJarlsbergian(itemId: Int): Boolean {
        if (itemId == MEDIOCRE_LAGER) return true
        val master = CoinmasterDatabase.findByNickname("jarl")
            ?: CoinmasterDatabase.findByShopId("jarl")
            ?: return false
        return master.buyRowFor(itemId) != null
    }
}
