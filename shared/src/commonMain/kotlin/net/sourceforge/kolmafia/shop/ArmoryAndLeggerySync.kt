package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ArmoryAndLeggeryRequest.visitShopRows] row + pulverized learn on armory visit. */
object ArmoryAndLeggerySync {

    const val SHOP_ID = ArmoryAndLeggeryShopRows.SHOP_ID

    private val ITEM_PATTERN = Regex(
        """<tr rel="(\d+)">.*?onClick='javascript:descitem\((\d+)\)'>.*?<b>(.*?)</b>.*?title="(.*?)".*?<b>([\d,]+)</b>.*?whichrow=(\d+)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun syncFromShopHtml(html: String, prefs: Preferences, force: Boolean = false) {
        val shopRows = parseShopRows(html)
        if (shopRows.isEmpty()) return

        var mutated = false
        val pulverizedSeen = mutableSetOf<Int>()

        for (shopRow in shopRows) {
            val costs = shopRow.costs
            if (costs.size != 1) continue
            val currency = costs[0]
            if (currency.isMeat) continue

            val item = shopRow.item
            val reward = StandardRewardDatabase.findStandardReward(item.itemId) ?: continue

            val pulverized = StandardRewardDatabase.findStandardPulverized(currency.itemId)
            if (force || pulverized == null) {
                if (!pulverizedSeen.contains(currency.itemId)) {
                    val pulverizedName =
                        ItemDatabase.getById(currency.itemId)?.name ?: reward.itemName
                    StandardRewardDatabase.registerStandardPulverized(
                        currency.itemId,
                        StandardRewardDatabase.StandardPulverized(
                            itemId = currency.itemId,
                            year = reward.year + 1,
                            hardcore = reward.hardcore,
                            itemName = pulverizedName,
                        ),
                    )
                    pulverizedSeen.add(currency.itemId)
                    mutated = true
                }
            }

            if (force || reward.row.equals("UNKNOWN", ignoreCase = true)) {
                StandardRewardDatabase.registerStandardReward(
                    item.itemId,
                    reward.copy(row = shopRow.rowId.toString()),
                )
                mutated = true
            }
        }

        if (mutated) {
            ArmoryAndLeggeryShopRows.rebuild()
        }
    }

    private fun parseShopRows(html: String): List<ShopRow> {
        val rows = mutableListOf<ShopRow>()
        for (match in ITEM_PATTERN.findAll(html)) {
            val itemId = match.groupValues[1].toIntOrNull() ?: continue
            val currencyName = match.groupValues[4].trim()
            val price = match.groupValues[5].replace(",", "").toIntOrNull() ?: continue
            val rowId = match.groupValues[6].toIntOrNull() ?: continue

            if (currencyName.equals("Meat", ignoreCase = true)) {
                rows.add(
                    ShopRow(
                        rowId = rowId,
                        item = ItemStack(itemId = itemId, count = 1),
                        costs = listOf(ItemStack(itemId = -1, count = price, isMeat = true)),
                    ),
                )
                continue
            }

            val currencyItem = ItemDatabase.getByName(currencyName) ?: continue
            rows.add(
                ShopRow(
                    rowId = rowId,
                    item = ItemStack(itemId = itemId, count = 1),
                    costs = listOf(ItemStack(itemId = currencyItem.id, count = price)),
                ),
            )
        }
        return rows
    }
}
