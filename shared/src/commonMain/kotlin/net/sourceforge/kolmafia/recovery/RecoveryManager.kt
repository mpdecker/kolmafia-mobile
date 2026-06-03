package net.sourceforge.kolmafia.recovery

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.RestoreData
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.data.RestoreType
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class RecoveryManager(
    private val inventoryManager: InventoryManager,
    private val skillManager: SkillManager,
    private val preferences: Preferences,
) {
    companion object {
        fun needsHpRecovery(state: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.AUTO_RECOVER_HP, true)) return false
            if (state.maxHp <= 0) return false
            val targetPct = prefs.getInt(Preferences.HP_RECOVERY_TARGET_PCT, 50)
            val ratioPct = state.currentHp * 100 / state.maxHp
            return ratioPct < targetPct
        }

        fun needsMpRecovery(state: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.AUTO_RECOVER_MP, false)) return false
            if (state.maxMp <= 0) return false
            val targetPct = prefs.getInt(Preferences.MP_RECOVERY_TARGET_PCT, 50)
            val ratioPct = state.currentMp * 100 / state.maxMp
            return ratioPct < targetPct
        }

        internal fun isFullRestore(restoreData: RestoreData): Boolean =
            restoreData.hpMaxExpr.contains("[") || restoreData.mpMaxExpr.contains("[")

        internal fun pickHpItem(
            restores: List<RestoreData>,
            items: Map<Int, InventoryItem>,
            nameToId: (String) -> Int?,
        ): InventoryItem? = restores
            .filter { it.type == RestoreType.ITEM && it.restoresHp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.hpMax }
            .firstNotNullOfOrNull { restore ->
                val id = nameToId(restore.name) ?: return@firstNotNullOfOrNull null
                items[id]?.takeIf { it.quantity > 0 }
            }

        internal fun pickMpItem(
            restores: List<RestoreData>,
            items: Map<Int, InventoryItem>,
            nameToId: (String) -> Int?,
        ): InventoryItem? = restores
            .filter { it.type == RestoreType.ITEM && it.restoresMp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.mpMax }
            .firstNotNullOfOrNull { restore ->
                val id = nameToId(restore.name) ?: return@firstNotNullOfOrNull null
                items[id]?.takeIf { it.quantity > 0 }
            }

        internal fun pickHpSkill(
            restores: List<RestoreData>,
            skills: List<SkillData>,
            currentMp: Int,
        ): SkillData? = restores
            .filter { it.type == RestoreType.SKILL && it.restoresHp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.hpMax }
            .firstNotNullOfOrNull { restore ->
                skills.firstOrNull { skill ->
                    skill.name.equals(restore.name, ignoreCase = true)
                        && skill.mpCost <= currentMp
                        && (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)
                }
            }

        internal fun pickMpSkill(
            restores: List<RestoreData>,
            skills: List<SkillData>,
            currentMp: Int,
        ): SkillData? = restores
            .filter { it.type == RestoreType.SKILL && it.restoresMp }
            .sortedByDescending { if (isFullRestore(it)) Int.MAX_VALUE else it.mpMax }
            .firstNotNullOfOrNull { restore ->
                skills.firstOrNull { skill ->
                    skill.name.equals(restore.name, ignoreCase = true)
                        && skill.mpCost <= currentMp
                        && (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)
                }
            }
    }

    suspend fun recoverIfNeeded(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean {
        var recovered = false
        if (needsHpRecovery(charState, preferences)) {
            recovered = recoverHp(charState, invState, skillState) || recovered
        }
        if (needsMpRecovery(charState, preferences)) {
            recovered = recoverMp(charState, invState, skillState) || recovered
        }
        return recovered
    }

    private suspend fun recoverHp(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean {
        val nameToId: (String) -> Int? = { name -> ItemDatabase.getByName(name)?.id }
        val item = pickHpItem(RestoreDatabase.hpRestores(), invState.items, nameToId)
        if (item != null) {
            inventoryManager.useItem(item)
            return true
        }
        val skill = pickHpSkill(RestoreDatabase.hpRestores(), skillState.skills, charState.currentMp)
        if (skill != null) {
            skillManager.cast(skill)
            return true
        }
        return false
    }

    private suspend fun recoverMp(
        charState: CharacterState,
        invState: InventoryState,
        skillState: SkillState,
    ): Boolean {
        val nameToId: (String) -> Int? = { name -> ItemDatabase.getByName(name)?.id }
        val item = pickMpItem(RestoreDatabase.mpRestores(), invState.items, nameToId)
        if (item != null) {
            inventoryManager.useItem(item)
            return true
        }
        val skill = pickMpSkill(RestoreDatabase.mpRestores(), skillState.skills, charState.currentMp)
        if (skill != null) {
            skillManager.cast(skill)
            return true
        }
        return false
    }
}
