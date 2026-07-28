package net.sourceforge.kolmafia.skill

import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillMaxLevel
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [ResponseTextParser.learnSkill] — local skill unlock without HTTP. */
object SkillLearner {

    /**
     * Learn or update a skill locally. Returns skill id when state changed, 0 when [firstLearnOnly]
     * skips an already-known skill.
     */
    fun learnSkill(
        skillId: Int,
        preferences: Preferences,
        skillManager: SkillManager?,
        inventoryManager: InventoryManager? = null,
        mpCostOverride: Int? = null,
        dailyNameOverride: String? = null,
        firstLearnOnly: Boolean = false,
    ): Int {
        val levelPref = "skillLevel$skillId"
        if (firstLearnOnly) {
            if (preferences.getInt(levelPref, 0) > 0) {
                return 0
            }
            preferences.setInt(levelPref, 1)
            mergeLocalSkill(skillId, skillManager, mpCostOverride, dailyNameOverride)
            return skillId
        }

        consumeItemsForSkill(skillId, inventoryManager)
        updateSkillLevelPref(skillId, preferences)

        mergeLocalSkill(skillId, skillManager, mpCostOverride, dailyNameOverride)
        return skillId
    }

    private fun consumeItemsForSkill(skillId: Int, inventoryManager: InventoryManager?) {
        val inv = inventoryManager ?: return
        fun hasItem(itemId: Int): Boolean =
            (inv.state.value.items[itemId]?.quantity ?: 0) > 0

        when (skillId) {
            BattleLearnSkillIds.SNARL_OF_THE_TIMBERWOLF -> {
                if (hasItem(BattleLearnSkillIds.TATTERED_WOLF_STANDARD)) {
                    inv.consumeItemLocally(BattleLearnSkillIds.TATTERED_WOLF_STANDARD)
                }
            }
            BattleLearnSkillIds.SPECTRAL_SNAPPER -> {
                if (hasItem(BattleLearnSkillIds.TATTERED_SNAKE_STANDARD)) {
                    inv.consumeItemLocally(BattleLearnSkillIds.TATTERED_SNAKE_STANDARD)
                }
            }
            BattleLearnSkillIds.SCARYSAUCE, BattleLearnSkillIds.FEARFUL_FETTUCINI -> {
                if (hasItem(BattleLearnSkillIds.ENGLISH_TO_A_F_U_E_DICTIONARY)) {
                    inv.consumeItemLocally(BattleLearnSkillIds.ENGLISH_TO_A_F_U_E_DICTIONARY)
                }
            }
            BattleLearnSkillIds.TANGO_OF_TERROR, BattleLearnSkillIds.DIRGE_OF_DREADFULNESS -> {
                if (hasItem(BattleLearnSkillIds.BIZARRE_ILLEGIBLE_SHEET_MUSIC)) {
                    inv.consumeItemLocally(BattleLearnSkillIds.BIZARRE_ILLEGIBLE_SHEET_MUSIC)
                }
            }
        }
    }

    private fun updateSkillLevelPref(skillId: Int, preferences: Preferences) {
        val levelPref = "skillLevel$skillId"
        when (skillId) {
            BattleLearnSkillIds.TOGGLE_OPTIMALITY,
            BattleLearnSkillIds.PIRATE_BELLOW,
            BattleLearnSkillIds.HOLIDAY_FUN,
            BattleLearnSkillIds.SUMMON_CARROT,
            BattleLearnSkillIds.BEAR_ESSENCE,
            BattleLearnSkillIds.CALCULATE_THE_UNIVERSE,
            BattleLearnSkillIds.EXPERIENCE_SAFARI,
            BattleLearnSkillIds.SUMMON_KOKOMO_RESORT_PASS,
            BattleLearnSkillIds.GENERATE_IRONY,
            -> preferences.incrementInt(levelPref)

            else -> {
                val maxLevel = SkillMaxLevel.getMaxLevel(skillId)
                if (maxLevel > 0) {
                    preferences.incrementInt(levelPref, delta = 1, max = maxLevel)
                }
            }
        }
    }

    private fun mergeLocalSkill(
        skillId: Int,
        skillManager: SkillManager?,
        mpCostOverride: Int?,
        dailyNameOverride: String?,
    ) {
        val definition = SkillDefinitionDatabase.getById(skillId) ?: return
        val skill = SkillData(
            id = skillId,
            name = dailyNameOverride ?: definition.name,
            type = definition.toSkillType(),
            mpCost = mpCostOverride ?: definition.mpCost,
            dailyLimit = 0,
            timesCast = 0,
        )
        skillManager?.learnLocalSkill(skill)
    }

    private fun SkillDefinition.toSkillType(): SkillType = when {
        isPassive -> SkillType.PASSIVE
        isCombat -> SkillType.COMBAT
        isNonCombat -> SkillType.NONCOMBAT
        isSong -> SkillType.BUFF
        else -> SkillType.OTHER
    }
}

private fun Preferences.incrementInt(key: String, delta: Int = 1, max: Int = 0) {
    var current = getInt(key, 0) + delta
    if (max > 0 && current >= max) {
        current = max
    }
    setInt(key, current)
}
