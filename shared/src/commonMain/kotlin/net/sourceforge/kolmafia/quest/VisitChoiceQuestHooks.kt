package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Dual-path visit-choice quest hooks shared by [AdventureManager] adventure-loop visits and
 * [GameRuntimeLibrary] raw visit_url processing (Phases 4471–4480).
 *
 * Keeps Doctor Bag / PirateRealm / Party Fair / Telegram-adjacent visit writers on one list so
 * both paths apply identical special-quest side effects.
 */
object VisitChoiceQuestHooks {

    /**
     * @return true when any visit writer matched and mutated state.
     */
    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase? = null,
    ): Boolean {
        if (preferences == null) return false
        var changed = false
        if (DoctorBagChoiceSync.applyVisit(choiceId, html, preferences)) changed = true
        if (PirateRealmSync.applyVisit(choiceId, html, preferences)) changed = true
        if (PartyFairChoiceSync.applyVisit(choiceId, html, preferences)) changed = true
        if (TelegramChoiceSync.applyVisit(choiceId, html, preferences, questDatabase)) changed = true
        return changed
    }
}
