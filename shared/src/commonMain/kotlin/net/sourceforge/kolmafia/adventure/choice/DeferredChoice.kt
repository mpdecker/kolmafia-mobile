package net.sourceforge.kolmafia.adventure.choice

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [ChoiceControl.registerDeferredChoice] high-traffic subset (Phases 1716–1730).
 */
object DeferredChoice {

    fun register(
        choice: Int,
        encounter: String = "",
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        processAdventureUsed: ((Int) -> Unit)? = null,
    ) {
        when (choice) {
            123 -> sessionLogger?.appendRawLine("[The Hidden Temple]")
            125 -> {
                processAdventureUsed?.invoke(1)
                sessionLogger?.appendRawLine("[The Hidden Temple]")
            }
            437 -> sessionLogger?.appendRawLine("[The Nemesis' Lair]")
            620, 621, 622, 634 -> {
                val loc = preferences?.getString(Preferences.LAST_LOCATION, "").orEmpty()
                if (loc.isNotBlank()) sessionLogger?.appendRawLine("[$loc]")
            }
            in 1005..1013 -> {
                val room = choice - 1004
                sessionLogger?.appendRawLine("[The Hedge Maze (Room $room)]")
            }
            1018, 1019 -> {
                processAdventureUsed?.invoke(1)
                sessionLogger?.appendRawLine("[The Black Forest]")
            }
            in 1223..1228 -> {
                val loc = preferences?.getString(Preferences.LAST_LOCATION, "").orEmpty()
                if (loc.isNotBlank()) sessionLogger?.appendRawLine("[$loc]")
            }
            1310 -> sessionLogger?.appendRawLine("[God Lobster]")
            1334, 1335, 1336 -> sessionLogger?.appendRawLine("[Boxing Daycare]")
            else -> Unit
        }
        if (encounter.isNotBlank()) {
            preferences?.setString("_lastDeferredChoiceEncounter", encounter)
        }
        preferences?.setInt("_lastDeferredChoice", choice)
    }
}
