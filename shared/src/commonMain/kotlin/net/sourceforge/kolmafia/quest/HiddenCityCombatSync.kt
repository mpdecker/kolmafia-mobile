package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [FightRequest] Ancient Protector Spirit / park pygmy relocate writers
 * + [QuestManager] pygmy witch accountant office unlock.
 */
object HiddenCityCombatSync {

    const val HIDDEN_APARTMENT = 341
    const val HIDDEN_HOSPITAL = 342
    const val HIDDEN_OFFICE = 343
    const val HIDDEN_BOWLING_ALLEY = 344
    const val HIDDEN_PARK = 345

    const val MCCLUSKY_FILE = 6689
    const val MCCLUSKY_FILE_PAGE5 = 7039

    fun applyCombatWin(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        adventureId: String,
        monster: String?,
        responseText: String,
        won: Boolean,
        ascensionNumber: Int = 0,
        itemCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (questDatabase == null || preferences == null || !won) return false
        val area = adventureId.toIntOrNull() ?: return false
        val monsterName = monster.orEmpty()
        var changed = false

        if (monsterName.contains("Ancient Protector Spirit", ignoreCase = true)) {
            when (area) {
                HIDDEN_APARTMENT -> {
                    if (preferences.getInt("hiddenApartmentProgress", 0) < 6) {
                        preferences.setInt("hiddenApartmentProgress", 6)
                        questDatabase.setProgress(Quest.CURSES, "step1")
                        changed = true
                    }
                }
                HIDDEN_HOSPITAL -> {
                    if (preferences.getInt("hiddenHospitalProgress", 0) < 6) {
                        preferences.setInt("hiddenHospitalProgress", 6)
                        questDatabase.setProgress(Quest.DOCTOR, "step1")
                        changed = true
                    }
                }
                HIDDEN_OFFICE -> {
                    if (preferences.getInt("hiddenOfficeProgress", 0) < 6) {
                        preferences.setInt("hiddenOfficeProgress", 6)
                        questDatabase.setProgress(Quest.BUSINESS, "step1")
                        changed = true
                    }
                }
                HIDDEN_BOWLING_ALLEY -> {
                    if (preferences.getInt("hiddenBowlingAlleyProgress", 0) < 6) {
                        preferences.setInt("hiddenBowlingAlleyProgress", 6)
                        questDatabase.setProgress(Quest.SPARE, "step1")
                        changed = true
                    }
                }
            }
        }

        if (area == HIDDEN_PARK) {
            if (monsterName.equals("pygmy janitor", ignoreCase = true)) {
                preferences.setInt("relocatePygmyJanitor", ascensionNumber)
                changed = true
            } else if (monsterName.equals("pygmy witch lawyer", ignoreCase = true)) {
                preferences.setInt("relocatePygmyLawyer", ascensionNumber)
                changed = true
            }
        }

        if (monsterName.equals("pygmy witch accountant", ignoreCase = true)) {
            if (itemCount(MCCLUSKY_FILE) == 0 &&
                itemCount(MCCLUSKY_FILE_PAGE5) == 0 &&
                preferences.getInt("hiddenOfficeProgress", 0) < 6 &&
                !responseText.contains("McClusky file", ignoreCase = true)
            ) {
                preferences.setInt("hiddenOfficeProgress", 6)
                changed = true
            }
        }
        return changed
    }
}
