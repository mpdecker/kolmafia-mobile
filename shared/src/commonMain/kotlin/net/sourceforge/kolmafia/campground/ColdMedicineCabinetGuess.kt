package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ColdMedicineChoiceSync

/**
 * Desktop [ColdMedicineCabinetCommand.guessCabinet] — headless expected cabinet contents.
 */
object ColdMedicineCabinetGuess {
    val ITEM_TYPES = listOf("equipment", "food", "booze", "potion", "pill")

    // Desktop ItemPool ids
    private const val ICE_CROWN = 10816
    private const val FROZEN_JEANS = 10817
    private const val ICE_WRAP = 10818
    private const val DOCS_FORTIFYING = 10824
    private const val DOCS_SMARTIFYING = 10825
    private const val DOCS_LIMBERING = 10826
    private const val DOCS_MEDICAL_GRADE = 10827
    private const val DOCS_SPECIAL_RESERVE = 10838
    private const val HOMEBODYL = 10828
    private const val EXTROVERMECTIN = 10829
    private const val BREATHITIN = 10830
    private const val FLESHAZOLE = 10831

    private val PILLS = mapOf(
        'i' to EXTROVERMECTIN,
        'o' to HOMEBODYL,
        'u' to BREATHITIN,
        'x' to FLESHAZOLE,
    )

    private val STAT_WINES = mapOf(
        "muscle" to DOCS_FORTIFYING,
        "mysticality" to DOCS_SMARTIFYING,
        "moxie" to DOCS_LIMBERING,
    )

    /** Map of cabinet slot → item id (null when unknown / not guessed). */
    fun guessCabinet(
        preferences: Preferences?,
        character: CharacterState?,
    ): Map<String, Int?> {
        return mapOf(
            "equipment" to guessNextEquipment(preferences),
            "food" to null,
            "booze" to guessNextWine(character),
            "potion" to null,
            "pill" to guessNextPill(preferences),
        )
    }

    fun guessNextEquipment(preferences: Preferences?): Int {
        return when (preferences?.getInt(ColdMedicineChoiceSync.EQUIPMENT_PREF, 0) ?: 0) {
            0 -> ICE_CROWN
            1 -> FROZEN_JEANS
            else -> ICE_WRAP
        }
    }

    fun guessNextPill(preferences: Preferences?): Int? {
        val env = preferences?.getString("lastCombatEnvironments", "").orEmpty()
        val counts = mutableMapOf<Char, Int>()
        for (c in env) {
            counts[c] = (counts[c] ?: 0) + 1
        }
        val unknown = counts['?'] ?: 0
        if (unknown > 10) return null
        for ((environment, count) in counts) {
            if (environment == '?') continue
            if (count > 10) return PILLS[environment]
            if (count + unknown > 10) return null
        }
        return PILLS['x']
    }

    fun guessNextWine(character: CharacterState?): Int {
        val cs = character ?: return DOCS_MEDICAL_GRADE
        // Desktop formula (points treated as subpoints via calculateBasePoints).
        val buffs = listOf(
            "muscle" to (KoLCharacter.calculateBasePoints(cs.buffedMusc.toLong()) - cs.baseMusc),
            "mysticality" to (KoLCharacter.calculateBasePoints(cs.buffedMyst.toLong()) - cs.baseMyst),
            "moxie" to (KoLCharacter.calculateBasePoints(cs.buffedMoxie.toLong()) - cs.baseMoxie),
        ).sortedBy { it.second }
        val top = buffs[2]
        val second = buffs[1]
        if (top.second == second.second) {
            return if (top.second > 5) DOCS_SPECIAL_RESERVE else DOCS_MEDICAL_GRADE
        }
        return STAT_WINES[top.first] ?: DOCS_MEDICAL_GRADE
    }

    fun itemName(itemId: Int?): String {
        if (itemId == null || itemId <= 0) return ""
        return ItemDatabase.getItemName(itemId) ?: ""
    }
}
