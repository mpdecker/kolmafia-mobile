package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Cold Medicine Cabinet choice 1455 —
 * visit consult/equipment parse + post consult increments.
 */
object ColdMedicineChoiceSync {

    const val CHOICE_ID = 1455

    const val CABINET_ITEM_ID = 10815
    const val CONSULTS_PREF = "_coldMedicineConsults"
    const val EQUIPMENT_PREF = "_coldMedicineEquipmentTaken"
    const val NEXT_CONSULT_PREF = "_nextColdMedicineConsult"
    const val MAX_CONSULTS = 5
    const val MAX_EQUIPMENT = 2

    private val CONSULT_PATTERN = Regex("""You have <b>(\d)</b> consul""", RegexOption.IGNORE_CASE)

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, CABINET_ITEM_ID)
        CONSULT_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { remaining ->
            preferences.setInt(CONSULTS_PREF, (MAX_CONSULTS - remaining).coerceAtLeast(0))
        }
        val equipmentCount = when {
            html.contains("ice crown", ignoreCase = true) -> 0
            html.contains("frozen jeans", ignoreCase = true) -> 1
            else -> 2
        }
        preferences.setInt(EQUIPMENT_PREF, equipmentCount)
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        turnsPlayed: Int = 0,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null || decision == 6) return false
        if (decision == 1) {
            val taken = preferences.getInt(EQUIPMENT_PREF, 0)
            preferences.setInt(EQUIPMENT_PREF, (taken + 1).coerceAtMost(MAX_EQUIPMENT))
        }
        val consults = preferences.getInt(CONSULTS_PREF, 0)
        preferences.setInt(CONSULTS_PREF, (consults + 1).coerceAtMost(MAX_CONSULTS))
        preferences.setInt(NEXT_CONSULT_PREF, turnsPlayed + 20)
        return true
    }
}
