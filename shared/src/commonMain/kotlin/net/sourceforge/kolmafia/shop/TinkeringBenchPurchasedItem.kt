package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint

/** Desktop [TinkeringBenchRequest.purchasedItem] ingredient checkpoint cleanup. */
object TinkeringBenchPurchasedItem {

    fun apply(master: CoinmasterData, itemId: Int, gameDatabase: GameDatabase?) {
        if (gameDatabase == null) return
        if (!master.nickname.equals("wereprofessor_tinker", ignoreCase = true) &&
            !master.shopId.equals("wereprofessor_tinker", ignoreCase = true)
        ) {
            return
        }
        val row = master.buyRowFor(itemId) ?: return
        for (cost in row.costs) {
            if (cost.isMeat || cost.itemId <= 0) continue
            OutfitCheckpoint.forgetEquipment(cost.itemId, gameDatabase)
        }
    }
}
