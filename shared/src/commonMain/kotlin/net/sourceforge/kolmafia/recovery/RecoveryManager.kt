package net.sourceforge.kolmafia.recovery

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager

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
    }
}
