package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/** Desktop [JunkMagazineRequest.visitShop] HIPPY quest bump. */
object JunkMagazineSync {

    fun syncFromShopHtml(prefs: Preferences) {
        val progress = prefs.getString(Quest.HIPPY.prefKey, QuestDatabase.UNSTARTED)
        if (QuestDatabase.stepOrdinal(progress) <= QuestDatabase.stepOrdinal("step1")) {
            prefs.setString(Quest.HIPPY.prefKey, "step2")
        }
    }
}
