package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [LightsOutManager] — Spookyraven Lights Out turn counter + now-check.
 * Choice sync remains in [net.sourceforge.kolmafia.quest.LightsOutChoiceSync].
 */
object LightsOutManager {

    const val COUNTER_LABEL = "Spookyraven Lights Out"
    const val COUNTER_IMAGE = "bulb.gif"
    const val TRACK_PREF = Preferences.TRACK_LIGHTS_OUT
    const val LAST_TURN_PREF = "lastLightsOutTurn"
    const val NEXT_ELIZABETH = "nextSpookyravenElizabethRoom"
    const val NEXT_STEPHEN = "nextSpookyravenStephenRoom"

    fun checkCounter(preferences: Preferences?, turnsPlayed: Int): Boolean {
        val prefs = preferences ?: return false
        if (!prefs.getBoolean(TRACK_PREF, false)) return false
        if (TurnCounter.isCounting(prefs, COUNTER_LABEL, turnsPlayed)) return false
        val elizabeth = prefs.getString(NEXT_ELIZABETH, "")
        val stephen = prefs.getString(NEXT_STEPHEN, "")
        if (elizabeth.equals("none", ignoreCase = true) &&
            stephen.equals("none", ignoreCase = true)
        ) {
            return false
        }
        if (elizabeth.isEmpty() && stephen.isEmpty()) return false
        val turns = 37 - (turnsPlayed % 37)
        TurnCounter.startCounting(prefs, turnsPlayed, turns, COUNTER_LABEL, COUNTER_IMAGE)
        return true
    }

    fun lightsOutNow(turnsPlayed: Int, lastLightsOutTurn: Int): Boolean =
        turnsPlayed > 0 &&
            turnsPlayed % 37 == 0 &&
            lastLightsOutTurn != turnsPlayed

    fun lightsOutNow(turnsPlayed: Int, preferences: Preferences?): Boolean =
        lightsOutNow(turnsPlayed, preferences?.getInt(LAST_TURN_PREF, 0) ?: 0)

    fun message(preferences: Preferences?, @Suppress("UNUSED_PARAMETER") link: Boolean = false): String {
        val prefs = preferences ?: return ""
        val parts = mutableListOf<String>()
        val elizabeth = prefs.getString(NEXT_ELIZABETH, "")
        if (elizabeth.isNotEmpty() && !elizabeth.equals("none", ignoreCase = true)) {
            parts += "Elizabeth can be found in $elizabeth."
        }
        val stephen = prefs.getString(NEXT_STEPHEN, "")
        if (stephen.isNotEmpty() && !stephen.equals("none", ignoreCase = true)) {
            parts += "Stephen can be found in $stephen."
        }
        return parts.joinToString("  ")
    }

    fun report(preferences: Preferences?): List<String> {
        val prefs = preferences ?: return emptyList()
        val lines = mutableListOf<String>()
        val elizabeth = prefs.getString(NEXT_ELIZABETH, "")
        lines += when {
            elizabeth.equals("none", ignoreCase = true) ->
                "You have defeated Elizabeth Spookyraven"
            elizabeth.isNotEmpty() ->
                "Elizabeth will next show up in $elizabeth"
            else -> "Elizabeth room unknown"
        }
        val stephen = prefs.getString(NEXT_STEPHEN, "")
        lines += when {
            stephen.equals("none", ignoreCase = true) ->
                "You have defeated Stephen Spookyraven"
            stephen.isNotEmpty() ->
                "Stephen will next show up in $stephen"
            else -> "Stephen room unknown"
        }
        return lines
    }
}
