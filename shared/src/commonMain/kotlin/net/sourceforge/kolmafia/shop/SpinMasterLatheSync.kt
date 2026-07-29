package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [SpinMasterLatheRequest.visitShop] pref sync. */
object SpinMasterLatheSync {

    const val SHOP_ID = "lathe"
    const val VISITED_PREF = "_spinmasterLatheVisited"

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        syncFromShopHtml(prefs)
    }

    fun syncFromShopHtml(prefs: Preferences) {
        prefs.setBoolean(VISITED_PREF, true)
    }
}
