package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [ArmoryAndLeggeryRequest.visitShopRows] row + pulverized learn on armory visit. */
object ArmoryAndLeggerySync {

    const val SHOP_ID = ArmoryAndLeggeryShopRows.SHOP_ID
    private const val STORE_NAME = ArmoryAndLeggeryShopRows.MASTER_NAME
    private const val LOG_DIVIDER = "--------------------"

    fun syncFromShopHtml(
        html: String,
        prefs: Preferences,
        force: Boolean = false,
        sessionLogger: SessionLogger? = null,
    ) {
        val shopRows = ShopRowParser.parseShop(html, includeMeat = true)
        if (shopRows.isEmpty()) return

        var mutated = false
        val pulverizedSeen = mutableSetOf<Int>()
        val pulverizeLines = mutableListOf<String>()
        val rewardLines = mutableListOf<String>()
        val meatLines = mutableListOf<String>()

        for (shopRow in shopRows) {
            val costs = shopRow.costs
            if (costs.size != 1) continue
            val currency = costs[0]

            if (currency.isMeat) {
                val item = shopRow.item
                val itemName = ItemDatabase.getById(item.itemId)?.name ?: continue
                if (NpcStoreVisitOverlay.registerMeatRow(
                        storeKey = SHOP_ID,
                        storeName = STORE_NAME,
                        itemId = item.itemId,
                        itemName = itemName,
                        price = currency.count,
                        rowId = shopRow.rowId,
                    )
                ) {
                    NpcStoreVisitOverlay.toNpcStoreLine(item.itemId)?.let { meatLines.add(it) }
                    mutated = true
                }
                continue
            }

            val item = shopRow.item
            val reward = StandardRewardDatabase.findStandardReward(item.itemId) ?: continue

            val pulverized = StandardRewardDatabase.findStandardPulverized(currency.itemId)
            if (force || pulverized == null) {
                if (!pulverizedSeen.contains(currency.itemId)) {
                    val pulverizedName =
                        ItemDatabase.getById(currency.itemId)?.name ?: reward.itemName
                    val registered = StandardRewardDatabase.StandardPulverized(
                        itemId = currency.itemId,
                        year = reward.year + 1,
                        hardcore = reward.hardcore,
                        itemName = pulverizedName,
                    )
                    StandardRewardDatabase.registerStandardPulverized(currency.itemId, registered)
                    pulverizeLines.add(StandardRewardDatabase.toData(registered))
                    pulverizedSeen.add(currency.itemId)
                    mutated = true
                }
            }

            if (force || reward.row.equals("UNKNOWN", ignoreCase = true)) {
                val updated = reward.copy(row = shopRow.rowId.toString())
                StandardRewardDatabase.registerStandardReward(item.itemId, updated)
                rewardLines.add(StandardRewardDatabase.toData(updated))
                mutated = true
            }
        }

        if (mutated) {
            logLearnedRows(sessionLogger, pulverizeLines, rewardLines, meatLines)
            StandardRewardRefresh.refreshArmoryRows()
        }
    }

    private fun logLearnedRows(
        sessionLogger: SessionLogger?,
        pulverizeLines: List<String>,
        rewardLines: List<String>,
        meatLines: List<String>,
    ) {
        if (sessionLogger == null) return
        if (pulverizeLines.isEmpty() && rewardLines.isEmpty() && meatLines.isEmpty()) return

        if (pulverizeLines.isNotEmpty()) {
            sessionLogger.appendRawLine(LOG_DIVIDER)
            sessionLogger.appendRawLines(pulverizeLines)
        }
        if (rewardLines.isNotEmpty()) {
            sessionLogger.appendRawLine(LOG_DIVIDER)
            sessionLogger.appendRawLines(rewardLines)
        }
        if (meatLines.isNotEmpty()) {
            sessionLogger.appendRawLine(LOG_DIVIDER)
            sessionLogger.appendRawLines(meatLines)
            sessionLogger.appendRawLine(LOG_DIVIDER)
        } else if (pulverizeLines.isNotEmpty() || rewardLines.isNotEmpty()) {
            sessionLogger.appendRawLine(LOG_DIVIDER)
        }
    }

}
