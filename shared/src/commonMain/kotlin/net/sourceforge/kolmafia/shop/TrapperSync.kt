package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.FINISHED

/** Desktop [TrapperRequest.visitShop] pref sync. */
object TrapperSync {

    fun syncFromShopHtml(html: String, prefs: Preferences, ascensionNumber: Int) {
        if (!html.contains("yeti furs", ignoreCase = true)) return
        prefs.setInt("lastTr4pz0rQuest", ascensionNumber)
        prefs.setString(Quest.TRAPPER.prefKey, FINISHED)
    }
}
