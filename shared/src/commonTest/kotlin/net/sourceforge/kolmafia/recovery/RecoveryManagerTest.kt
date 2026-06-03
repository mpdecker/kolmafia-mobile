package net.sourceforge.kolmafia.recovery

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecoveryManagerTest {

    private fun prefs(vararg pairs: Pair<String, Any>): Preferences {
        val settings = MapSettings()
        pairs.forEach { (k, v) ->
            when (v) {
                is Boolean -> settings.putBoolean(k, v)
                is Int     -> settings.putInt(k, v)
                is String  -> settings.putString(k, v)
            }
        }
        return Preferences(settings)
    }

    private fun state(hp: Int, maxHp: Int, mp: Int = 0, maxMp: Int = 0) =
        CharacterState(currentHp = hp, maxHp = maxHp, currentMp = mp, maxMp = maxMp)

    // ── HP needs-recovery ────────────────────────────────────────────────────

    @Test fun hpRecovery_disabledByPref_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to false)
        assertFalse(RecoveryManager.needsHpRecovery(state(10, 100), p))
    }

    @Test fun hpRecovery_aboveTarget_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_TARGET_PCT to 50)
        assertFalse(RecoveryManager.needsHpRecovery(state(60, 100), p))
    }

    @Test fun hpRecovery_atTarget_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_TARGET_PCT to 50)
        assertFalse(RecoveryManager.needsHpRecovery(state(50, 100), p))
    }

    @Test fun hpRecovery_belowTarget_returnsTrue() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_TARGET_PCT to 50)
        assertTrue(RecoveryManager.needsHpRecovery(state(49, 100), p))
    }

    @Test fun hpRecovery_defaultTargetIs50Pct() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true)
        assertTrue(RecoveryManager.needsHpRecovery(state(40, 100), p))
        assertFalse(RecoveryManager.needsHpRecovery(state(60, 100), p))
    }

    @Test fun hpRecovery_zeroMaxHp_doesNotCrash() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_TARGET_PCT to 50)
        assertFalse(RecoveryManager.needsHpRecovery(state(0, 0), p))
    }

    // ── MP needs-recovery ────────────────────────────────────────────────────

    @Test fun mpRecovery_disabledByPref_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to false)
        assertFalse(RecoveryManager.needsMpRecovery(state(0, 0, 10, 100), p))
    }

    @Test fun mpRecovery_aboveTarget_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_TARGET_PCT to 30)
        assertFalse(RecoveryManager.needsMpRecovery(state(0, 0, 40, 100), p))
    }

    @Test fun mpRecovery_belowTarget_returnsTrue() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_TARGET_PCT to 30)
        assertTrue(RecoveryManager.needsMpRecovery(state(0, 0, 20, 100), p))
    }
}
