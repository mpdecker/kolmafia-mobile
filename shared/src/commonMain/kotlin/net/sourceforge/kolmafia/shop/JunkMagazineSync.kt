package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [JunkMagazineRequest.visitShop] HIPPY quest bump. */
object JunkMagazineSync {

    const val SHOP_ID = "junkmagazine"

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
        val progress = prefs.getString(Quest.HIPPY.prefKey, QuestDatabase.UNSTARTED)
        if (QuestDatabase.stepOrdinal(progress) <= QuestDatabase.stepOrdinal("step1")) {
            prefs.setString(Quest.HIPPY.prefKey, "step2")
        }
    }
}
