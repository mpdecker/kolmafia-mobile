package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] FantasyRealm hour/area unlock choices 1280–1307
 * (skips 1287 and 1306 — no desktop cases).
 * Place visit unlocks remain in [FantasyRealmSync].
 */
object FantasyRealmChoiceSync {

    val CHOICE_IDS = setOf(
        1280, 1281, 1282, 1283, 1284, 1285, 1286,
        1288, 1289, 1290, 1291, 1292, 1293, 1294, 1295, 1296, 1297, 1298, 1299,
        1300, 1301, 1302, 1303, 1304, 1305, 1307,
    )

    const val FR_KEY = 9844
    const val FR_PURPLE_MUSHROOM = 9845
    const val FR_TAINTED_MARSHMALLOW = 9846
    const val FR_CHESWICKS_NOTES = 9847
    const val FR_DRAGON_ORE = 9851
    const val FR_POISONED_SMORE = 9853
    const val FR_DRUIDIC_ORB = 9854
    const val FR_HOLY_WATER = 9856
    const val FR_CHESWICKS_COMPASS = 9865
    const val FR_ARREST_WARRANT = 9866
    const val FR_CHARGED_ORB = 9895
    const val FR_NOTARIZED_WARRANT = 9897

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId !in CHOICE_IDS || preferences == null) return false
        return when (choiceId) {
            1280 -> applyWelcome(decision, preferences)
            1281 -> applyCrossroads(decision, preferences)
            1282 -> applyMountains(decision, preferences, consumeItem)
            1283 -> applyWood(decision, preferences)
            1284 -> applySwamp(decision, preferences, consumeItem)
            1285 -> applyVillage(decision, preferences, consumeItem)
            1286 -> applyCemetery(decision, preferences)
            1288 -> applyLeaveArea(decision, preferences, "The Old Rubee Mine,", leaveDecision = 6, burnHours = true)
            1289 -> {
                var changed = applyLeaveArea(decision, preferences, "The Foreboding Cave,", leaveDecision = 6, burnHours = true)
                if (decision == 3) {
                    appendArea(preferences, "The Lair of the Phoenix,")
                    changed = true
                }
                changed
            }
            1290 -> {
                var changed = applyLeaveArea(decision, preferences, "The Faerie Cyrkle,", leaveDecision = 6, burnHours = true)
                if (decision == 3) {
                    appendArea(preferences, "The Spider Queen's Lair,")
                    changed = true
                }
                changed
            }
            1291 -> {
                var changed = applyLeaveArea(decision, preferences, "The Druidic Campsite,", leaveDecision = 6, burnHours = true)
                if (decision == 2) {
                    consumeItem(FR_TAINTED_MARSHMALLOW, 1)
                    changed = true
                }
                changed
            }
            1292 -> {
                var changed = applyLeaveArea(decision, preferences, "Near the Witch's House,", leaveDecision = 6, burnHours = true)
                if (decision == 2) {
                    consumeItem(FR_PURPLE_MUSHROOM, 1)
                    changed = true
                }
                changed
            }
            1293 -> applyLeaveArea(decision, preferences, "The Evil Cathedral,", leaveDecision = 6, burnHours = true)
            1294 -> {
                var changed = applyLeaveArea(decision, preferences, "The Barrow Mounds,", leaveDecision = 6, burnHours = true)
                if (decision == 3) {
                    appendArea(preferences, "The Ghoul King's Catacomb,")
                    changed = true
                }
                changed
            }
            1295 -> applyLeaveArea(
                decision, preferences, "The Cursed Village Thieves' Guild,", leaveDecision = 6, burnHours = true,
            )
            1296 -> {
                var changed = applyLeaveArea(decision, preferences, "The Troll Fortress,", leaveDecision = 6, burnHours = true)
                if (decision == 3) {
                    consumeItem(FR_CHESWICKS_NOTES, 1)
                    changed = true
                }
                changed
            }
            1297 -> applyLeaveArea(decision, preferences, "The Labyrinthine Crypt,", leaveDecision = 6, burnHours = true)
            1298 -> {
                var changed = applyLeaveArea(decision, preferences, "The Lair of the Phoenix,", leaveDecision = 6, burnHours = false)
                if (decision == 1) {
                    consumeItem(FR_HOLY_WATER, 1)
                    changed = true
                }
                changed
            }
            1299 -> applyLeaveArea(decision, preferences, "The Dragon's Moor,", leaveDecision = 6, burnHours = false)
            1300 -> applyLeaveArea(decision, preferences, "Duke Vampire's Chateau,", leaveDecision = 6, burnHours = false)
            1301 -> applyLeaveArea(decision, preferences, "The Spider Queen's Lair,", leaveDecision = 6, burnHours = false)
            1302 -> {
                var changed = applyLeaveArea(decision, preferences, "The Archwizard's Tower,", leaveDecision = 6, burnHours = false)
                if (decision == 1) {
                    consumeItem(FR_CHARGED_ORB, 1)
                    changed = true
                }
                changed
            }
            1303 -> {
                var changed = applyLeaveArea(decision, preferences, "The Ley Nexus,", leaveDecision = 6, burnHours = false)
                if (decision == 1) {
                    consumeItem(FR_CHESWICKS_COMPASS, 1)
                    changed = true
                }
                changed
            }
            1304 -> applyLeaveArea(decision, preferences, "The Ghoul King's Catacomb,", leaveDecision = 6, burnHours = false)
            1305 -> {
                var changed = applyLeaveArea(decision, preferences, "The Ogre Chieftain's Keep,", leaveDecision = 6, burnHours = false)
                if (decision == 1) {
                    consumeItem(FR_POISONED_SMORE, 1)
                    changed = true
                }
                changed
            }
            1307 -> {
                var changed = applyLeaveArea(decision, preferences, "The Master Thief's Chalet,", leaveDecision = 6, burnHours = false)
                if (decision == 1) {
                    consumeItem(FR_NOTARIZED_WARRANT, 1)
                    changed = true
                }
                changed
            }
            else -> false
        }
    }

    private fun applyWelcome(decision: Int, preferences: Preferences): Boolean {
        if (decision == 6) return false
        preferences.setInt("_frHoursLeft", 5)
        val unlocks = StringBuilder("The Bandit Crossroads,")
        if (preferences.getBoolean("frMountainsUnlocked", false)) unlocks.append("The Towering Mountains,")
        if (preferences.getBoolean("frWoodUnlocked", false)) unlocks.append("The Mystic Wood,")
        if (preferences.getBoolean("frSwampUnlocked", false)) unlocks.append("The Putrid Swamp,")
        if (preferences.getBoolean("frVillageUnlocked", false)) unlocks.append("The Cursed Village,")
        if (preferences.getBoolean("frCemetaryUnlocked", false)) unlocks.append("The Sprawling Cemetery,")
        preferences.setString("_frAreasUnlocked", unlocks.toString())
        return true
    }

    private fun applyCrossroads(decision: Int, preferences: Preferences): Boolean {
        val unlocks = StringBuilder(preferences.getString("_frAreasUnlocked", ""))
        if (decision != 6) {
            decrementHours(preferences)
            singleStringReplace(unlocks, "The Bandit Crossroads,")
        }
        when (decision) {
            1 -> unlocks.append("The Towering Mountains,")
            2 -> unlocks.append("The Mystic Wood,")
            3 -> unlocks.append("The Putrid Swamp,")
            4 -> unlocks.append("The Cursed Village,")
            5 -> unlocks.append("The Sprawling Cemetery,")
        }
        preferences.setString("_frAreasUnlocked", unlocks.toString())
        return true
    }

    private fun applyMountains(
        decision: Int,
        preferences: Preferences,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        val unlocks = StringBuilder(preferences.getString("_frAreasUnlocked", ""))
        if (decision != 11) {
            decrementHours(preferences)
            singleStringReplace(unlocks, "The Towering Mountains,")
        }
        when (decision) {
            1 -> {
                unlocks.append("The Old Rubee Mine,")
                consumeItem(FR_KEY, 1)
            }
            2 -> unlocks.append("The Foreboding Cave,")
            3 -> unlocks.append("The Master Thief's Chalet,")
            4 -> consumeItem(FR_DRUIDIC_ORB, 1)
            5 -> unlocks.append("The Ogre Chieftain's Keep,")
            10 -> preferences.setInt("_frButtonsPressed", preferences.getInt("_frButtonsPressed", 0) + 1)
        }
        preferences.setString("_frAreasUnlocked", unlocks.toString())
        return true
    }

    private fun applyWood(decision: Int, preferences: Preferences): Boolean {
        val unlocks = StringBuilder(preferences.getString("_frAreasUnlocked", ""))
        if (decision != 11) {
            decrementHours(preferences)
            singleStringReplace(unlocks, "The Mystic Wood,")
        }
        when (decision) {
            1 -> unlocks.append("The Faerie Cyrkle,")
            2 -> unlocks.append("The Druidic Campsite,")
            3 -> unlocks.append("The Ley Nexus,")
            10 -> preferences.setInt("_frButtonsPressed", preferences.getInt("_frButtonsPressed", 0) + 1)
        }
        preferences.setString("_frAreasUnlocked", unlocks.toString())
        return true
    }

    private fun applySwamp(
        decision: Int,
        preferences: Preferences,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        val unlocks = StringBuilder(preferences.getString("_frAreasUnlocked", ""))
        if (decision != 11) {
            decrementHours(preferences)
            singleStringReplace(unlocks, "The Putrid Swamp,")
        }
        when (decision) {
            1 -> unlocks.append("Near the Witch's House,")
            2 -> {
                unlocks.append("The Troll Fortress,")
                consumeItem(FR_KEY, 1)
            }
            3 -> unlocks.append("The Dragon's Moor,")
            10 -> preferences.setInt("_frButtonsPressed", preferences.getInt("_frButtonsPressed", 0) + 1)
        }
        preferences.setString("_frAreasUnlocked", unlocks.toString())
        return true
    }

    private fun applyVillage(
        decision: Int,
        preferences: Preferences,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        val unlocks = StringBuilder(preferences.getString("_frAreasUnlocked", ""))
        if (decision != 11) {
            decrementHours(preferences)
            singleStringReplace(unlocks, "The Cursed Village,")
        }
        when (decision) {
            1 -> unlocks.append("The Evil Cathedral,")
            2 -> unlocks.append("The Cursed Village Thieves' Guild,")
            3 -> unlocks.append("The Archwizard's Tower,")
            6 -> consumeItem(FR_DRAGON_ORE, 1)
            7 -> consumeItem(FR_ARREST_WARRANT, 1)
            10 -> preferences.setInt("_frButtonsPressed", preferences.getInt("_frButtonsPressed", 0) + 1)
        }
        preferences.setString("_frAreasUnlocked", unlocks.toString())
        return true
    }

    private fun applyCemetery(decision: Int, preferences: Preferences): Boolean {
        val unlocks = StringBuilder(preferences.getString("_frAreasUnlocked", ""))
        if (decision != 11) {
            decrementHours(preferences)
            singleStringReplace(unlocks, "The Sprawling Cemetery,")
        }
        when (decision) {
            1 -> unlocks.append("The Labyrinthine Crypt,")
            2 -> unlocks.append("The Barrow Mounds,")
            3 -> unlocks.append("Duke Vampire's Chateau,")
            10 -> preferences.setInt("_frButtonsPressed", preferences.getInt("_frButtonsPressed", 0) + 1)
        }
        preferences.setString("_frAreasUnlocked", unlocks.toString())
        return true
    }

    private fun applyLeaveArea(
        decision: Int,
        preferences: Preferences,
        area: String,
        leaveDecision: Int,
        burnHours: Boolean,
    ): Boolean {
        val unlocks = StringBuilder(preferences.getString("_frAreasUnlocked", ""))
        if (decision != leaveDecision) {
            if (burnHours) decrementHours(preferences)
            singleStringReplace(unlocks, area)
            preferences.setString("_frAreasUnlocked", unlocks.toString())
            return true
        }
        preferences.setString("_frAreasUnlocked", unlocks.toString())
        return decision == leaveDecision
    }

    private fun appendArea(preferences: Preferences, area: String) {
        val unlocks = StringBuilder(preferences.getString("_frAreasUnlocked", ""))
        unlocks.append(area)
        preferences.setString("_frAreasUnlocked", unlocks.toString())
    }

    private fun decrementHours(preferences: Preferences) {
        preferences.setInt("_frHoursLeft", (preferences.getInt("_frHoursLeft", 0) - 1).coerceAtLeast(0))
    }

    /** Desktop [StringUtilities.singleStringReplace] — first occurrence only. */
    private fun singleStringReplace(buffer: StringBuilder, search: String) {
        val idx = buffer.indexOf(search)
        if (idx >= 0) buffer.deleteRange(idx, idx + search.length)
    }
}
