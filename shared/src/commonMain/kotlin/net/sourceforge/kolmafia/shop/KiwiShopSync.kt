package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [MiniKiwiShopRequest.visitShop] pref sync. */
object KiwiShopSync {

    const val SHOP_ID = "kiwi"

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
        prefs.setBoolean(
            "_miniKiwiIntoxicatingSpiritsBought",
            !html.contains("mini kiwi intoxicating spirits", ignoreCase = true),
        )
    }
}
