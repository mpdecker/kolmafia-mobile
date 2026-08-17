package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Hidden City choice progress writers (780–789, 791).
 * Automation stays in [HiddenCityHandlers]; this only writes prefs/quest steps.
 */
object HiddenCityChoiceSync {

    const val MOSS_COVERED_STONE_SPHERE = 6697
    const val DRIPPING_STONE_SPHERE = 6698
    const val CRACKLING_STONE_SPHERE = 6699
    const val SCORCHED_STONE_SPHERE = 6700
    const val SIX_BALL = 1905
    const val TWO_BALL = 1901
    const val ONE_BALL = 1900
    const val FIVE_BALL = 1904
    const val BOWLING_BALL = 6696

    fun applyVisitChoice(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        var changed = false
        when (choiceId) {
            781 -> {
                if (!html.contains("option value=1") &&
                    preferences.getInt("hiddenApartmentProgress", 0) == 0
                ) {
                    preferences.setInt("hiddenApartmentProgress", 1)
                    changed = true
                }
            }
            783 -> {
                if (!html.contains("option value=1") &&
                    preferences.getInt("hiddenHospitalProgress", 0) == 0
                ) {
                    preferences.setInt("hiddenHospitalProgress", 1)
                    changed = true
                }
            }
            785 -> {
                if (!html.contains("option value=1") &&
                    preferences.getInt("hiddenOfficeProgress", 0) == 0
                ) {
                    preferences.setInt("hiddenOfficeProgress", 1)
                    changed = true
                }
            }
            787 -> {
                if (!html.contains("option value=1") &&
                    preferences.getInt("hiddenBowlingAlleyProgress", 0) == 0
                ) {
                    preferences.setInt("hiddenBowlingAlleyProgress", 1)
                    changed = true
                }
            }
            791 -> {
                preferences.setInt("zigguratLianas", 1)
                changed = true
            }
        }
        return changed
    }

    fun applyPostChoice(
        choiceId: Int,
        html: String,
        decision: Int,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        ascensionNumber: Int = 0,
        consumeItem: (Int) -> Unit = {},
        itemCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        var changed = false
        when (choiceId) {
            780 -> {
                if (decision == 1 && html.contains("penthouse is empty now")) {
                    if (preferences.getInt("hiddenApartmentProgress", 0) < 7) {
                        preferences.setInt("hiddenApartmentProgress", 7)
                        changed = true
                    }
                } else if (decision == 3) {
                    preferences.setInt("relocatePygmyLawyer", ascensionNumber)
                    changed = true
                } else if (decision == 4) {
                    preferences.setBoolean("candyCaneSwordApartmentBuilding", true)
                    changed = true
                }
            }
            781 -> {
                when (decision) {
                    1 -> {
                        preferences.setInt("hiddenApartmentProgress", 1)
                        questDatabase.setProgress(Quest.CURSES, QuestDatabase.STARTED)
                        changed = true
                    }
                    2 -> {
                        consumeItem(MOSS_COVERED_STONE_SPHERE)
                        preferences.setInt("hiddenApartmentProgress", 8)
                        changed = true
                    }
                    3 -> {
                        consumeItem(SIX_BALL)
                        changed = true
                    }
                }
            }
            783 -> {
                when (decision) {
                    1 -> {
                        preferences.setInt("hiddenHospitalProgress", 1)
                        questDatabase.setProgress(Quest.DOCTOR, QuestDatabase.STARTED)
                        changed = true
                    }
                    2 -> {
                        consumeItem(DRIPPING_STONE_SPHERE)
                        preferences.setInt("hiddenHospitalProgress", 8)
                        changed = true
                    }
                    3 -> {
                        consumeItem(TWO_BALL)
                        changed = true
                    }
                }
            }
            785 -> {
                when (decision) {
                    1 -> {
                        preferences.setInt("hiddenOfficeProgress", 1)
                        questDatabase.setProgress(Quest.BUSINESS, QuestDatabase.STARTED)
                        changed = true
                    }
                    2 -> {
                        consumeItem(CRACKLING_STONE_SPHERE)
                        preferences.setInt("hiddenOfficeProgress", 8)
                        changed = true
                    }
                    3 -> {
                        consumeItem(ONE_BALL)
                        changed = true
                    }
                    4 -> {
                        preferences.setBoolean("_candyCaneSwordOvergrownShrine", true)
                        changed = true
                    }
                }
            }
            786 -> {
                if (decision == 1 && html.contains("boss's office is empty")) {
                    if (preferences.getInt("hiddenOfficeProgress", 0) < 7) {
                        preferences.setInt("hiddenOfficeProgress", 7)
                        changed = true
                    }
                } else if (decision == 2 &&
                    !html.contains("boring binder clip") &&
                    itemCount(HiddenCityCombatSync.MCCLUSKY_FILE) == 0 &&
                    itemCount(HiddenCityCombatSync.MCCLUSKY_FILE_PAGE5) == 0 &&
                    itemCount(7040) == 0 &&
                    preferences.getInt("hiddenOfficeProgress", 0) < 6
                ) {
                    preferences.setInt("hiddenOfficeProgress", 6)
                    changed = true
                }
            }
            787 -> {
                when (decision) {
                    1 -> {
                        preferences.setInt("hiddenBowlingAlleyProgress", 1)
                        questDatabase.setProgress(Quest.SPARE, QuestDatabase.STARTED)
                        changed = true
                    }
                    2 -> {
                        consumeItem(SCORCHED_STONE_SPHERE)
                        preferences.setInt("hiddenBowlingAlleyProgress", 8)
                        changed = true
                    }
                    3 -> {
                        consumeItem(FIVE_BALL)
                        changed = true
                    }
                }
            }
            788 -> {
                if (decision == 1 && html.contains("without a frustrated ghost to torment")) {
                    if (preferences.getInt("hiddenBowlingAlleyProgress", 0) < 7) {
                        preferences.setInt("hiddenBowlingAlleyProgress", 7)
                        changed = true
                    }
                }
                if (decision == 1) {
                    consumeItem(BOWLING_BALL)
                    changed = true
                }
                if (decision == 1 || decision == 2) {
                    val bowlCount = preferences.getInt("hiddenBowlingAlleyProgress", 0)
                    if (bowlCount < 6) {
                        preferences.setInt(
                            "hiddenBowlingAlleyProgress",
                            if (bowlCount < 2) 2 else bowlCount + 1,
                        )
                        changed = true
                    }
                }
                if (decision == 2) {
                    preferences.setBoolean("candyCaneSwordBowlingAlley", true)
                    changed = true
                }
            }
            789 -> {
                if (decision == 2) {
                    preferences.setInt("relocatePygmyJanitor", ascensionNumber)
                    changed = true
                }
            }
        }
        return changed
    }
}
