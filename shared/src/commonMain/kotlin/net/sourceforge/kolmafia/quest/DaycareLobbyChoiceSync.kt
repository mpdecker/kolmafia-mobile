package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Boxing Daycare Lobby 1334 + Spa 1335.
 * Defers full Daycare 1336 recruit/scavenge pile.
 */
object DaycareLobbyChoiceSync {

    const val LOBBY_CHOICE = 1334
    const val SPA_CHOICE = 1335

    val CHOICE_IDS = setOf(LOBBY_CHOICE, SPA_CHOICE)

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            LOBBY_CHOICE -> when {
                decision == 1 -> {
                    preferences.setBoolean("_daycareNap", true)
                    true
                }
                decision == 2 && html.contains("only allowed one spa treatment") -> {
                    preferences.setBoolean("_daycareSpa", true)
                    true
                }
                else -> false
            }
            SPA_CHOICE -> {
                if (decision == 5) return false
                preferences.setBoolean("_daycareSpa", true)
                true
            }
            else -> false
        }
    }
}
