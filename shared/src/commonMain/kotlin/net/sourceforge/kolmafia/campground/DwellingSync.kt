package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [CampgroundRequest.parseDwelling] housing-type detection. */
object DwellingSync {

    const val BIG_ROCK_ITEM_ID = 30
    const val CURRENT_DWELLING_ITEM_ID_PREF = "_currentDwellingItemId"
    private const val TOILET_PAPER_ITEM_ID = 1923

    private val HOUSING_PATTERN =
        Regex("""/rest([\da-z])(tp)?(_free)?.gif""", RegexOption.IGNORE_CASE)

    private val DWELLING_CODES = mapOf(
        0 to BIG_ROCK_ITEM_ID,
        1 to 69, // NEWBIESPORT_TENT
        2 to 73, // BARSKIN_TENT
        3 to 143, // COTTAGE
        4 to 526, // HOUSE
        5 to 3127, // SANDCASTLE
        6 to 3374, // TWIG_HOUSE
        7 to 3416, // HOBO_FORTRESS
        8 to 4347, // GINGERBREAD_HOUSE
        9 to 4485, // BRICKO_PYRAMID
        10 to 4771, // GINORMOUS_PUMPKIN
        11 to 6668, // GIANT_FARADAY_CAGE
        12 to 7089, // SNOW_FORT
        13 to 7295, // ELEVENT
        14 to 7758, // RESIDENCE_CUBE
        15 to 9185, // GIANT_PILGRIM_HAT
        16 to 10497, // HOUSE_SIZED_MUSHROOM
        17 to 11600, // MINI_KIWI_TIPI
    )

    fun currentDwellingItemId(prefs: Preferences?): Int {
        val stored = prefs?.getInt(CURRENT_DWELLING_ITEM_ID_PREF, -1) ?: -1
        return if (stored >= 0) stored else BIG_ROCK_ITEM_ID
    }

    fun applyFromHtml(html: String, prefs: Preferences?) {
        if (prefs == null) return
        val match = HOUSING_PATTERN.find(html) ?: return
        val code = parseDwellingCode(match.groupValues[1]) ?: return
        val itemId = DWELLING_CODES[code] ?: return
        prefs.setInt(CURRENT_DWELLING_ITEM_ID_PREF, itemId)
        if (match.groupValues[2].isNotEmpty()) {
            CampgroundInventorySync.setItem(prefs, TOILET_PAPER_ITEM_ID, 1)
        }
    }

    internal fun parseDwellingCode(raw: String): Int? {
        if (raw.isEmpty()) return null
        val first = raw.first()
        return if (first.isDigit()) {
            raw.toIntOrNull()
        } else {
            first.code - 'a'.code + 10
        }
    }
}
