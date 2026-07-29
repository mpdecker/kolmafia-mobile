package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.FINISHED
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.UNSTARTED
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [BlackMarketRequest.visitShop] MACGUFFIN step1 unlock. */
object BlackMarketShopSync {

    const val SHOP_ID = "blackmarket"

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        syncFromShopVisit(prefs, state)
    }

    fun syncFromShopVisit(prefs: Preferences, state: CharacterState?) {
        val charState = state ?: CharacterState()
        if (prefs.getInt("lastWuTangDefeated", -1) == charState.ascensionNumber) return
        if (charState.inNuclearAutumn) return
        val progress = prefs.getString(Quest.MACGUFFIN.prefKey, UNSTARTED)
        if (progress == FINISHED || progress.contains("step")) return
        prefs.setString(Quest.MACGUFFIN.prefKey, "step1")
    }
}
