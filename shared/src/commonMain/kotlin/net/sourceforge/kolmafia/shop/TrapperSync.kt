package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.FINISHED
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [TrapperRequest.visitShop] pref sync. */
object TrapperSync {

    const val SHOP_ID = "trapper"

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        syncFromShopHtml(html, prefs, state?.ascensionNumber ?: 0)
    }

    fun syncFromShopHtml(html: String, prefs: Preferences, ascensionNumber: Int) {
        if (!html.contains("yeti furs", ignoreCase = true)) return
        prefs.setInt("lastTr4pz0rQuest", ascensionNumber)
        prefs.setString(Quest.TRAPPER.prefKey, FINISHED)
    }
}
