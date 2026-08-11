package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [GenericRequest] dispensary password unlock pref writer. */
object DispensarySync {

    const val LAST_DISPENSARY_OPEN_PREF = "lastDispensaryOpen"

    fun applyFromResponse(html: String, state: CharacterState?, preferences: Preferences?) {
        if (state == null || preferences == null) return
        if (!html.contains("FARQUAR", ignoreCase = true) &&
            !html.contains("Sleeping Near the Enemy", ignoreCase = true)
        ) {
            return
        }
        preferences.setInt(LAST_DISPENSARY_OPEN_PREF, state.ascensionNumber)
    }
}
