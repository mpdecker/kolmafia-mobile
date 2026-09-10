package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [InterestingCoinRequest.visitShop] / [purchasedItem] daily + ascension caps.
 */
object InterestingCoinShopSync {

    const val SHOP_ID = "interesting"

    private val DAILY_ITEM_IDS = intArrayOf(
        12312, 12311, 12310, 12305, 12306, 12307, 12294, 12292, 12308, 12295,
    )
    private val ASCENSION_ITEM_IDS = intArrayOf(
        12293, 12303, 12299, 12298, 12300, 12301,
    )

    fun dailyProperty(itemId: Int): String = "_itemBoughtPerDay$itemId"
    fun ascensionProperty(itemId: Int): String = "itemBoughtPerAscension$itemId"

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        syncFromShopHtml(html, prefs)
    }

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        for (itemId in DAILY_ITEM_IDS) {
            val name = ItemDatabase.getItemName(itemId)
            if (name.isBlank() || !html.contains(name, ignoreCase = true)) {
                prefs.setInt(dailyProperty(itemId), 3)
            }
        }
        for (itemId in ASCENSION_ITEM_IDS) {
            val name = ItemDatabase.getItemName(itemId)
            val absent = name.isBlank() || !html.contains(name, ignoreCase = true)
            prefs.setBoolean(ascensionProperty(itemId), absent)
        }
    }

    fun purchasedItem(itemId: Int, prefs: Preferences) {
        when {
            itemId in DAILY_ITEM_IDS -> prefs.increment(dailyProperty(itemId), 1, 3)
            itemId in ASCENSION_ITEM_IDS -> prefs.setBoolean(ascensionProperty(itemId), true)
        }
    }

    fun canBuyItem(itemId: Int, prefs: Preferences): Boolean = when {
        itemId in DAILY_ITEM_IDS -> prefs.getInt(dailyProperty(itemId), 0) < 3
        itemId in ASCENSION_ITEM_IDS -> !prefs.getBoolean(ascensionProperty(itemId), false)
        else -> true
    }
}
