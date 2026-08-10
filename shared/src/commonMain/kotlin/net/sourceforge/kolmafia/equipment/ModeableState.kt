package net.sourceforge.kolmafia.equipment

import net.sourceforge.kolmafia.preferences.Preferences

/** Reads live modeable state from character prefs (desktop Modeable.getState()). */
object ModeableState {

    fun currentModes(preferences: Preferences?): Map<Modeable, String> =
        Modeable.entries.associateWith { currentMode(preferences, it) }

    fun currentMode(preferences: Preferences?, modeable: Modeable): String {
        if (preferences == null) return modeable.modes.first()
        val raw = when (modeable) {
            Modeable.RETROCAPE -> {
                val hero = preferences.getString("retroCapeSuperhero", "").trim()
                val wash = preferences.getString("retroCapeWashingInstructions", "").trim()
                "$hero $wash".trim()
            }
            else -> {
                val pref = modeable.statePref ?: return modeable.modes.first()
                preferences.getString(pref, "").trim()
            }
        }
        return modeable.normalizeMode(raw) ?: modeable.modes.first()
    }
}
