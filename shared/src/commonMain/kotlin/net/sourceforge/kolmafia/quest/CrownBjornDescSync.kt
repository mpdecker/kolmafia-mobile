package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.familiar.FamiliarData

/** Desktop [ItemDatabase.parseCrownOfThrones] / [parseBuddyBjorn]. */
object CrownBjornDescSync {

    const val CROWN_ITEM_ID = 4614
    const val BJORN_ITEM_ID = 7200

    private val OCCUPANT_PATTERN =
        Regex("""Current Occupant:.*?<b>.* the (.*?)</b>""", RegexOption.DOT_MATCHES_ALL)

    data class Occupant(val id: Int, val race: String)

    val CLEARED = Occupant(id = 0, race = "")

    fun parseOccupant(html: String, ownedFamiliars: List<FamiliarData>): Occupant {
        val race = OCCUPANT_PATTERN.find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (race.isEmpty()) return CLEARED
        val familiar = ownedFamiliars.find { it.race.equals(race, ignoreCase = true) }
            ?: return CLEARED
        return Occupant(id = familiar.id, race = familiar.race)
    }

    fun isCrownItem(itemId: Int): Boolean = itemId == CROWN_ITEM_ID

    fun isBjornItem(itemId: Int): Boolean = itemId == BJORN_ITEM_ID
}
