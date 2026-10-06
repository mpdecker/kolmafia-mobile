package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ItemPool.eliminationProcessor] for bang potions / slime vials —
 * sets `lastBangPotion*` / `lastSlimeVial*` prefs and fills the last unknown by elimination.
 */
object BangPotionElimination {

    /** name, combat message, inventory-use message */
    val BANG_POTION_STRINGS: Array<Array<String>> = arrayOf(
        arrayOf("inebriety", "wino", "liquid fire"),
        arrayOf("healing", "better", "You gain"),
        arrayOf("confusion", "confused", "Confused"),
        arrayOf("blessing", "stylish", "Izchak's Blessing"),
        arrayOf("detection", "blink", "Object Detection"),
        arrayOf("sleepiness", "yawn", "Sleepy"),
        arrayOf("mental acuity", "smarter", "Strange Mental Acuity"),
        arrayOf("ettin strength", "stronger", "Strength of Ten Ettins"),
        arrayOf("teleportitis", "disappearing", "Teleportitis"),
    )

    /** Primary / secondary / tertiary slime vial (name, inventory-use message). */
    val SLIME_VIAL_STRINGS: Array<Array<Array<String>>> = arrayOf(
        arrayOf(
            arrayOf("strong", "Slimily Strong"),
            arrayOf("sagacious", "Slimily Sagacious"),
            arrayOf("speedy", "Slimily Speedy"),
        ),
        arrayOf(
            arrayOf("brawn", "Bilious Brawn"),
            arrayOf("brains", "Bilious Brains"),
            arrayOf("briskness", "Bilious Briskness"),
        ),
        arrayOf(
            arrayOf("slimeform", "Slimeform"),
            arrayOf("eyesight", "Ichorous Eyesight"),
            arrayOf("intensity", "Ichorous Intensity"),
            arrayOf("muscle", "Mucilaginous Muscle"),
            arrayOf("mentalism", "Mucilaginous Mentalism"),
            arrayOf("moxiousness", "Mucilaginous Moxiousness"),
        ),
    )

    const val FIRST_BANG = ItemDatabase.FIRST_BANG_POTION
    const val LAST_BANG = ItemDatabase.LAST_BANG_POTION
    const val VIAL_RED = 3885
    const val VIAL_BLUE = 3887
    const val VIAL_ORANGE = 3888
    const val VIAL_VIOLET = 3890
    const val VIAL_VERMILION = 3891
    const val VIAL_PURPLE = 3896

    fun identifyBangPotion(html: String, itemId: Int, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        if (itemId !in FIRST_BANG..LAST_BANG) return false
        for (i in BANG_POTION_STRINGS.indices) {
            if (html.contains(BANG_POTION_STRINGS[i][2])) {
                return eliminationProcessor(
                    BANG_POTION_STRINGS, i, itemId, FIRST_BANG, LAST_BANG,
                    "lastBangPotion", " of ", prefs,
                )
            }
        }
        return false
    }

    fun identifySlimeVial(html: String, itemId: Int, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        val (strings, minId, maxId) = when (itemId) {
            in VIAL_RED..VIAL_BLUE -> Triple(SLIME_VIAL_STRINGS[0], VIAL_RED, VIAL_BLUE)
            in VIAL_ORANGE..VIAL_VIOLET -> Triple(SLIME_VIAL_STRINGS[1], VIAL_ORANGE, VIAL_VIOLET)
            in VIAL_VERMILION..VIAL_PURPLE -> Triple(SLIME_VIAL_STRINGS[2], VIAL_VERMILION, VIAL_PURPLE)
            else -> return false
        }
        for (i in strings.indices) {
            if (html.contains(strings[i][1])) {
                return eliminationProcessor(
                    strings, i, itemId, minId, maxId,
                    "lastSlimeVial", ": ", prefs,
                )
            }
        }
        return false
    }

    fun eliminationProcessor(
        strings: Array<Array<String>>,
        index: Int,
        id: Int,
        minId: Int,
        maxId: Int,
        baseName: String,
        @Suppress("UNUSED_PARAMETER") joiner: String,
        preferences: Preferences,
    ): Boolean {
        var effect = strings[index][0]
        preferences.setString(baseName + id, effect)

        val possibilities = strings.map { it[0] }.toMutableSet()
        var missing = 0
        for (i in minId..maxId) {
            effect = preferences.getString(baseName + i, "")
            if (effect.isEmpty()) {
                if (missing != 0) return false
                missing = i
            } else {
                possibilities.remove(effect)
            }
        }
        if (missing == 0 || possibilities.size != 1) return false
        preferences.setString(baseName + missing, possibilities.first())
        return true
    }
}
