package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.campground.GardenCropSync
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] The Mushy Center choice 1410 —
 * fertilize/pick crop level + visit HTML level parse.
 */
object MushyCenterChoiceSync {

    const val CHOICE_ID = 1410

    const val LEVEL_PREF = "mushroomGardenCropLevel"
    const val VISITED_PREF = "_mushroomGardenVisited"
    const val MAX_LEVEL = 11

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val messageLevel = when {
            html.contains("walk around inside") -> 11
            html.contains("immense mushroom") -> 5
            html.contains("giant mushroom") -> 4
            html.contains("bulky mushroom") -> 3
            html.contains("plump mushroom") -> 2
            html.contains("decent-sized mushroom") -> 1
            else -> 0
        }
        val imageLevel = when {
            html.contains("mushgrow5.gif") -> 5
            html.contains("mushgrow4.gif") -> 4
            html.contains("mushgrow3.gif") -> 3
            html.contains("mushgrow2.gif") -> 2
            html.contains("mushgrow1.gif") -> 1
            else -> 0
        }
        val parsed = maxOf(messageLevel, imageLevel)
        if (parsed <= 0) return false
        val current = preferences.getInt(LEVEL_PREF, 0)
        val newLevel = maxOf(parsed, current)
        preferences.setInt(LEVEL_PREF, newLevel)
        GardenCropSync.setMushroomCropLevel(preferences, newLevel)
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val mushroomLevel = when (decision) {
            1 -> {
                val next = (preferences.getInt(LEVEL_PREF, 0) + 1).coerceAtMost(MAX_LEVEL)
                preferences.setInt(LEVEL_PREF, next)
                next
            }
            2 -> {
                preferences.setInt(LEVEL_PREF, 1)
                1
            }
            else -> return false
        }
        GardenCropSync.setMushroomCropLevel(preferences, mushroomLevel)
        preferences.setBoolean(VISITED_PREF, true)
        return true
    }
}
