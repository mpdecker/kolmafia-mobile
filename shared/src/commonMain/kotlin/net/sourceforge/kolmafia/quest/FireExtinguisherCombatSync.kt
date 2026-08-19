package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CryptManager

/**
 * Desktop [FightRequest] industrial fire extinguisher skills 7386–7390.
 */
object FireExtinguisherCombatSync {

    const val CHARGE_PREF = "_fireExtinguisherCharge"
    const val HAREM_ADVENTURE = 259
    const val SMUT_ORC_ADVENTURE = 295
    const val DESERT_ADVENTURE = 364

    private val BATHOLE_IDS = setOf(30, 31, 32, 33, 34)
    private val CYRPT_IDS = setOf(
        CryptManager.DEFILED_ALCOVE,
        CryptManager.DEFILED_CRANNY,
        CryptManager.DEFILED_NICHE,
        CryptManager.DEFILED_NOOK,
    )

    fun apply(
        html: String,
        adventureId: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase? = null,
    ): Boolean {
        if (preferences == null || html.isBlank()) return false
        var changed = false
        if (html.contains("both comical and immobile")) {
            decrementCharge(preferences, 5)
            changed = true
        }
        if (html.contains("You fire a blast of frigid extinguishant at your foe")) {
            decrementCharge(preferences, 10)
            changed = true
        }
        if (html.contains("create a suit made of flame-retardant foam")) {
            decrementCharge(preferences, 10)
            changed = true
        }
        if (html.contains("dust and debris is kicked up into a cyclone")) {
            decrementCharge(preferences, 5)
            changed = true
        }
        if (applyZoneSpecific(html, adventureId, preferences, questDatabase)) {
            changed = true
        }
        return changed
    }

    private fun applyZoneSpecific(
        html: String,
        adventureId: String,
        preferences: Preferences,
        questDatabase: QuestDatabase?,
    ): Boolean {
        val id = adventureId.toIntOrNull() ?: return false
        var success = false
        when {
            id in BATHOLE_IDS &&
                html.contains("You squeeze down the nozzle on your fire extinguisher and release a blast") -> {
                if (questDatabase != null && !questDatabase.isQuestLaterThan(Quest.BAT, "step2")) {
                    questDatabase.advanceQuest(Quest.BAT)
                }
                preferences.setBoolean("fireExtinguisherBatHoleUsed", true)
                success = true
            }
            id in CYRPT_IDS &&
                html.contains("The chill of the refrigerant quickly replaces some of the chill of evil") -> {
                CryptManager.decreaseEvilness(id, 10, preferences)
                preferences.setBoolean("fireExtinguisherCyrptUsed", true)
                success = true
            }
            id == HAREM_ADVENTURE && html.contains("You fill the harem with foam") -> {
                preferences.setBoolean("fireExtinguisherHaremUsed", true)
                success = true
            }
            id == SMUT_ORC_ADVENTURE &&
                html.contains("You wantonly spray the area with your fire extinguisher") -> {
                val next = (preferences.getInt(SmutOrcCombatSync.PREF, 0) + 11)
                    .coerceAtMost(SmutOrcCombatSync.MAX_PROGRESS)
                preferences.setInt(SmutOrcCombatSync.PREF, next)
                preferences.setBoolean("fireExtinguisherChasmUsed", true)
                success = true
            }
            id == DESERT_ADVENTURE &&
                html.contains("You aim the nozzle directly into your mouth") -> {
                preferences.setBoolean("fireExtinguisherDesertUsed", true)
                success = true
            }
        }
        if (success) decrementCharge(preferences, 20)
        return success
    }

    private fun decrementCharge(preferences: Preferences, delta: Int) {
        preferences.setInt(
            CHARGE_PREF,
            (preferences.getInt(CHARGE_PREF, 0) - delta).coerceAtLeast(0),
        )
    }
}
