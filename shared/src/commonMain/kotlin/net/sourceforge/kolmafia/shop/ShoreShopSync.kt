package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [ShoreGiftShopRequest.visitShop] cheap toaster pref sync. */
object ShoreShopSync {

    const val SHOP_ID = "shore"
    const val CHEAP_TOASTER_BOUGHT_PREF = "itemBoughtPerAscension637"

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
            CHEAP_TOASTER_BOUGHT_PREF,
            !html.contains("cheap toaster", ignoreCase = true),
        )
    }
}
