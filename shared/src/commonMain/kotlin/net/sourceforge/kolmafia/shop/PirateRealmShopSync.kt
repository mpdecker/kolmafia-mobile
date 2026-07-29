package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [PirateRealmFunALogRequest.visitShop] Fun-a-log unlock pref sync. */
object PirateRealmShopSync {

    const val SHOP_ID = "piraterealm"

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        FunALogUnlockPrefs.syncFromShopHtml(html, prefs)
    }

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        FunALogUnlockPrefs.syncFromShopHtml(html, prefs)
    }
}
