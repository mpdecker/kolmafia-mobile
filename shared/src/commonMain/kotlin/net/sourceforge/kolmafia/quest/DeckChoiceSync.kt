package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest

/**
 * Desktop [ChoiceControl] / [DeckOfEveryCardRequest] for Deck of Every Card:
 * - preChoice draw increments (`_deckCardsDrawn`)
 * - visit cheat dropdown → `_deckCardsSeen`
 * - draw encounter append → `_deckCardsSeen`
 */
object DeckChoiceSync {

    const val RANDOM_CHOICE = DeckOfEveryCardRequest.RANDOM_CHOICE
    const val CHEAT_CHOICE = DeckOfEveryCardRequest.CHEAT_CHOICE

    const val DRAWS_PREF = DeckOfEveryCardRequest.DRAWS_PREF
    const val SEEN_PREF = DeckOfEveryCardRequest.SEEN_PREF
    const val MAX_DRAWS = 15

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            CHEAT_CHOICE -> DeckOfEveryCardRequest.parseAvailableCards(html, preferences)
            RANDOM_CHOICE -> DeckOfEveryCardRequest.parseCardEncounter(html, preferences) != null
            else -> false
        }
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        var changed = false
        if (decision == 1) {
            val delta = when (choiceId) {
                RANDOM_CHOICE -> 1
                CHEAT_CHOICE -> 4
                else -> 0
            }
            if (delta > 0) {
                val current = preferences.getInt(DRAWS_PREF, 0)
                preferences.setInt(DRAWS_PREF, (current + delta).coerceAtMost(MAX_DRAWS))
                changed = true
            }
        }
        if (choiceId == RANDOM_CHOICE || choiceId == CHEAT_CHOICE) {
            if (DeckOfEveryCardRequest.parseCardEncounter(html, preferences) != null) {
                changed = true
            }
            if (choiceId == CHEAT_CHOICE) {
                if (DeckOfEveryCardRequest.parseAvailableCards(html, preferences)) {
                    changed = true
                }
            }
        }
        return changed
    }
}
