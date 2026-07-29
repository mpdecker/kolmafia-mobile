package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [InternetMemeShopRequest.visitShop] pref sync. */
object BaconShopSync {

    const val SHOP_ID = "bacon"

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
        prefs.setBoolean("_internetViralVideoBought", !html.contains("viral video", ignoreCase = true))
        prefs.setBoolean("_internetPlusOneBought", !html.contains("plus one", ignoreCase = true))
        prefs.setBoolean("_internetGallonOfMilkBought", !html.contains("gallon of milk", ignoreCase = true))
        prefs.setBoolean(
            "_internetPrintScreenButtonBought",
            !html.contains("print screen button", ignoreCase = true),
        )
        prefs.setBoolean(
            "_internetDailyDungeonMalwareBought",
            !html.contains("daily dungeon malware", ignoreCase = true),
        )
    }
}
