package net.sourceforge.kolmafia.recovery

import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [RecoveryManager.isRecoveryPossible] / [RecoveryManager.runThresholdChecks]
 * (Phases 2016–2030).
 */
object RecoveryGates {

    /**
     * @param currentRound fight round (0 = not in combat)
     * @param inMultiFight desktop FightRequest.inMultiFight
     * @param choiceFollowsFight desktop FightRequest.choiceFollowsFight
     * @param handlingChoice adventure loop currently resolving a choice
     * @param canWalkAway ChoiceManager.canWalkAway equivalent
     * @param recoveryActive RecoveryManager.isRecoveryActive
     */
    fun isRecoveryPossible(
        character: CharacterState?,
        recoveryActive: Boolean,
        currentRound: Int = 0,
        inMultiFight: Boolean = false,
        choiceFollowsFight: Boolean = false,
        handlingChoice: Boolean = false,
        canWalkAway: Boolean = true,
    ): Boolean {
        if (recoveryActive) return false
        if (currentRound != 0) return false
        if (inMultiFight) return false
        if (choiceFollowsFight) return false
        if (handlingChoice && !canWalkAway) return false
        if (CharpaneValhallaSync.inValhalla) return false
        val mode = character?.limitMode.orEmpty()
        if (LimitModeGates.limitRecovery(mode)) return false
        return true
    }

    /**
     * Desktop [RecoveryManager.runThresholdChecks] — false when HP at/below
     * [Preferences.AUTO_ABORT_THRESHOLD] × maxHP (threshold < 0 disables).
     */
    fun runThresholdChecks(character: CharacterState?, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return true
        val cs = character ?: return true
        val autoStop = prefs.getFloat(Preferences.AUTO_ABORT_THRESHOLD, -1f)
        if (autoStop < 0f) return true
        if (cs.maxHp <= 0) return true
        val floor = autoStop * cs.maxHp
        return cs.currentHp > floor
    }
}
