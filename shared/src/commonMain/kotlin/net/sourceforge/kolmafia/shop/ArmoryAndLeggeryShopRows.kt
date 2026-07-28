package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.StandardRewardDatabase

/** Desktop [ArmoryAndLeggeryRequest.initializeShopRows] dynamic standard-reward buy rows. */
object ArmoryAndLeggeryShopRows {

    const val SHOP_ID = "armory"
    const val MASTER_NAME = "Armory and Leggery"

    fun buildStandardRewardRows(): List<ShopRow> {
        val rows = mutableListOf<ShopRow>()
        for ((itemId, reward) in StandardRewardDatabase.allStandardRewards()) {
            if (reward.row.equals("UNKNOWN", ignoreCase = true)) continue
            val rowId = StandardRewardDatabase.parseRowNumber(reward.row) ?: continue
            val currencyId = StandardRewardDatabase.findPulverization(reward)
            if (currencyId == -1) continue
            rows.add(
                ShopRow(
                    rowId = rowId,
                    item = ItemStack(itemId = itemId, count = 1),
                    costs = listOf(ItemStack(itemId = currencyId, count = 1)),
                ),
            )
        }
        return rows
    }

    fun rebuild() {
        CoinmasterDatabase.registerOrReplaceArmory(buildStandardRewardRows())
    }
}
