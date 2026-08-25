package net.sourceforge.kolmafia.recovery

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecoveryGatesTest {

    @Test
    fun isRecoveryPossible_falseWhenInFight() {
        assertFalse(
            RecoveryGates.isRecoveryPossible(
                character = CharacterState(),
                recoveryActive = false,
                currentRound = 2,
            ),
        )
    }

    @Test
    fun isRecoveryPossible_falseWhenRecoveryActive() {
        assertFalse(
            RecoveryGates.isRecoveryPossible(
                character = CharacterState(),
                recoveryActive = true,
            ),
        )
    }

    @Test
    fun isRecoveryPossible_trueWhenIdle() {
        assertTrue(
            RecoveryGates.isRecoveryPossible(
                character = CharacterState(),
                recoveryActive = false,
                currentRound = 0,
            ),
        )
    }

    @Test
    fun runThresholdChecks_abortsBelowFraction() {
        val prefs = Preferences(MapSettings().apply {
            putString(Preferences.AUTO_ABORT_THRESHOLD, "0.3")
        })
        val low = CharacterState(currentHp = 20, maxHp = 100)
        assertFalse(RecoveryGates.runThresholdChecks(low, prefs))
        val high = CharacterState(currentHp = 50, maxHp = 100)
        assertTrue(RecoveryGates.runThresholdChecks(high, prefs))
    }

    @Test
    fun runThresholdChecks_disabledWhenNegative() {
        val prefs = Preferences(MapSettings().apply {
            putString(Preferences.AUTO_ABORT_THRESHOLD, "-1")
        })
        val low = CharacterState(currentHp = 1, maxHp = 100)
        assertTrue(RecoveryGates.runThresholdChecks(low, prefs))
    }
}
