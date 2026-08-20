package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Villain Lair choices:
 * - 1260 A Strange Panel (color)
 * - 1261 Which Door?
 * - 1262 What Setting? (symbology)
 */
object VillainLairChoiceSync {

    const val CHOICE_DOOR = 1261
    const val CHOICE_PANEL = 1260
    const val CHOICE_SETTING = 1262

    /** @deprecated Use [CHOICE_DOOR]; kept for AshP726 callers. */
    const val CHOICE_ID = CHOICE_DOOR

    const val BORIS_KEY = 282
    const val JARLSBERG_KEY = 283
    const val SNEAKY_PETE_KEY = 284

    const val KEY_PREF = "_villainLairKey"
    const val PROGRESS_PREF = "_villainLairProgress"
    const val DOOR_USED_PREF = "_villainLairDoorChoiceUsed"
    const val COLOR_USED_PREF = "_villainLairColorChoiceUsed"
    const val SYMBOLOGY_USED_PREF = "_villainLairSymbologyChoiceUsed"

    private val PANEL_PLUS_10 = listOf(
        "10 casualties", "10 crew", "10 minions", "10 ski", "10 members",
        "ten techs", "10 soldiers",
    )
    private val PANEL_PLUS_5 = listOf(
        "5 casualties", "5 souls", "5 minions", "five minions", "group of ski",
        "5 members", "five people", "five of us",
    )
    private val SETTING_PLUS_20 = listOf(
        "20 of the", "20 minions", "20 or so", "20 soldiers",
    )
    private val SETTING_PLUS_10 = listOf(
        "10 or so", "10 injured", "10 patrol-sicles", "10 soldiers",
    )
    private val SETTING_MINUS_15 = listOf(
        "15 aquanats", "15 reserve", "15 previously", "15 Soldiers",
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_DOOR || preferences == null) return false
        val key = detectKey(html) ?: return false
        preferences.setString(KEY_PREF, key)
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        visitHtml: String? = null,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean = when (choiceId) {
        CHOICE_PANEL -> applyPanel(decision, html, preferences)
        CHOICE_SETTING -> applySetting(decision, html, preferences)
        CHOICE_DOOR -> applyDoor(decision, html, preferences, visitHtml, consumeItem)
        else -> false
    }

    private fun applyPanel(
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null || decision !in 1..3) return false
        val delta = when {
            PANEL_PLUS_10.any { html.contains(it) } -> 10
            PANEL_PLUS_5.any { html.contains(it) } -> 5
            else -> -7
        }
        bumpProgress(preferences, delta)
        preferences.setBoolean(COLOR_USED_PREF, true)
        return true
    }

    private fun applySetting(
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null || decision !in 1..3) return false
        when {
            SETTING_PLUS_20.any { html.contains(it) } -> bumpProgress(preferences, 20)
            SETTING_PLUS_10.any { html.contains(it) } -> bumpProgress(preferences, 10)
            SETTING_MINUS_15.any { html.contains(it) } -> bumpProgress(preferences, -15)
        }
        preferences.setBoolean(SYMBOLOGY_USED_PREF, true)
        return true
    }

    private fun applyDoor(
        decision: Int,
        html: String,
        preferences: Preferences?,
        visitHtml: String?,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (preferences == null) return false
        val source = visitHtml?.takeIf { it.isNotEmpty() } ?: html
        detectKey(source)?.let { preferences.setString(KEY_PREF, it) }

        return when (decision) {
            1 -> {
                if (!html.contains("drop 1000")) return false
                bumpProgress(preferences, 10)
                preferences.setBoolean(DOOR_USED_PREF, true)
                true
            }
            2 -> {
                if (!html.contains("insert the key")) return false
                keyItemId(preferences.getString(KEY_PREF, ""))?.let { consumeItem(it, 1) }
                bumpProgress(preferences, 15)
                preferences.setBoolean(DOOR_USED_PREF, true)
                true
            }
            3 -> {
                bumpProgress(preferences, -13)
                preferences.setBoolean(DOOR_USED_PREF, true)
                true
            }
            else -> false
        }
    }

    private fun detectKey(html: String): String? = when {
        html.contains("Boris") -> "boris"
        html.contains("Jarlsberg") -> "jarlsberg"
        html.contains("Sneaky Pete") -> "pete"
        else -> null
    }

    private fun keyItemId(key: String): Int? = when (key) {
        "boris" -> BORIS_KEY
        "jarlsberg" -> JARLSBERG_KEY
        "pete" -> SNEAKY_PETE_KEY
        else -> null
    }

    private fun bumpProgress(preferences: Preferences, delta: Int) {
        val current = preferences.getInt(PROGRESS_PREF, 0)
        preferences.setInt(PROGRESS_PREF, current + delta)
    }
}
