package net.sourceforge.kolmafia.recovery

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test fun mpRecovery_zeroMaxMp_doesNotCrash() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_TARGET_PCT to 50)
        assertFalse(RecoveryManager.needsMpRecovery(state(0, 0, 0, 0), p))
    }

    // ── HP item selection ────────────────────────────────────────────────────

    @Test fun pickHpItem_noItems_returnsNull() {
        val result = RecoveryManager.pickHpItem(
            restores = listOf(restore("aspirin", hpMax = 101)),
            items = emptyMap(),
            nameToId = { 1 },
        )
        assertNull(result)
    }

    @Test fun pickHpItem_itemInInventory_returnsIt() {
        val aspirin = invItem(id = 99, count = 5)
        val result = RecoveryManager.pickHpItem(
            restores = listOf(restore("aspirin", hpMax = 101)),
            items = mapOf(99 to aspirin),
            nameToId = { name -> if (name == "aspirin") 99 else null },
        )
        assertNotNull(result)
        assertEquals(99, result.itemId)
    }

    @Test fun pickHpItem_noCount_returnsNull() {
        val result = RecoveryManager.pickHpItem(
            restores = listOf(restore("aspirin", hpMax = 101)),
            items = mapOf(99 to invItem(id = 99, count = 0)),
            nameToId = { 99 },
        )
        assertNull(result)
    }

    @Test fun pickHpItem_prefersFullRestoreOverPartial() {
        val partial = invItem(id = 1, count = 3)
        val full = invItem(id = 2, count = 1)
        val result = RecoveryManager.pickHpItem(
            restores = listOf(
                restore("partial", hpMax = 101),
                restore("full-restore", hpMaxExpr = "[HP]"),
            ),
            items = mapOf(1 to partial, 2 to full),
            nameToId = { name -> if (name == "partial") 1 else 2 },
        )
        assertNotNull(result)
        assertEquals(2, result.itemId)
    }

    // ── MP item selection ────────────────────────────────────────────────────

    @Test fun pickMpItem_itemPresent_returnsIt() {
        val soda = invItem(id = 50, count = 2)
        val result = RecoveryManager.pickMpItem(
            restores = listOf(mpRestore("blue pixel potion", mpMax = 80)),
            items = mapOf(50 to soda),
            nameToId = { 50 },
        )
        assertNotNull(result)
        assertEquals(50, result.itemId)
    }

    // ── HP skill selection ───────────────────────────────────────────────────

    @Test fun pickHpSkill_sufficientMp_returnsSkill() {
        val result = RecoveryManager.pickHpSkill(
            restores = listOf(skillRestore("Cannelloni Cocoon", hpMaxExpr = "[HP]")),
            skills = listOf(skill(id = 3012, name = "Cannelloni Cocoon", mpCost = 40)),
            currentMp = 50,
        )
        assertNotNull(result)
        assertEquals(3012, result.id)
    }

    @Test fun pickHpSkill_insufficientMp_returnsNull() {
        val result = RecoveryManager.pickHpSkill(
            restores = listOf(skillRestore("Cannelloni Cocoon", hpMaxExpr = "[HP]")),
            skills = listOf(skill(id = 3012, name = "Cannelloni Cocoon", mpCost = 40)),
            currentMp = 10,
        )
        assertNull(result)
    }

    @Test fun pickHpSkill_dailyLimitReached_returnsNull() {
        val result = RecoveryManager.pickHpSkill(
            restores = listOf(skillRestore("Cannelloni Cocoon", hpMaxExpr = "[HP]")),
            skills = listOf(skill(id = 3012, name = "Cannelloni Cocoon", mpCost = 40, dailyLimit = 1, timesCast = 1)),
            currentMp = 100,
        )
        assertNull(result)
    }

    // ── Test helpers ─────────────────────────────────────────────────────────

    private fun restore(
        name: String,
        hpMin: Int = 0,
        hpMax: Int = 0,
        hpMinExpr: String = hpMin.toString(),
        hpMaxExpr: String = hpMax.toString(),
    ) = net.sourceforge.kolmafia.data.RestoreData(
        name = name,
        type = net.sourceforge.kolmafia.data.RestoreType.ITEM,
        hpMinExpr = hpMinExpr,
        hpMaxExpr = hpMaxExpr,
        mpMinExpr = "0",
        mpMaxExpr = "0",
        advCost = 0,
        usesLeftExpr = "",
        notes = "",
    )

    private fun mpRestore(name: String, mpMin: Int = 0, mpMax: Int = 0) =
        net.sourceforge.kolmafia.data.RestoreData(
            name = name,
            type = net.sourceforge.kolmafia.data.RestoreType.ITEM,
            hpMinExpr = "0", hpMaxExpr = "0",
            mpMinExpr = mpMin.toString(), mpMaxExpr = mpMax.toString(),
            advCost = 0, usesLeftExpr = "", notes = "",
        )

    private fun skillRestore(
        name: String,
        hpMin: Int = 0,
        hpMaxExpr: String = "0",
    ) = net.sourceforge.kolmafia.data.RestoreData(
        name = name,
        type = net.sourceforge.kolmafia.data.RestoreType.SKILL,
        hpMinExpr = hpMin.toString(),
        hpMaxExpr = hpMaxExpr,
        mpMinExpr = "0", mpMaxExpr = "0",
        advCost = 0, usesLeftExpr = "", notes = "",
    )

    private fun invItem(id: Int, count: Int) =
        net.sourceforge.kolmafia.inventory.InventoryItem(
            itemId = id, name = "Item #$id", quantity = count,
            type = net.sourceforge.kolmafia.inventory.ItemType.OTHER,
        )

    private fun skill(
        id: Int,
        name: String,
        mpCost: Int = 0,
        dailyLimit: Int = 0,
        timesCast: Int = 0,
    ) = net.sourceforge.kolmafia.skill.SkillData(
        id = id, name = name,
        type = net.sourceforge.kolmafia.skill.SkillType.PASSIVE,
        mpCost = mpCost, dailyLimit = dailyLimit, timesCast = timesCast,
    )

    // ── Stop-threshold helpers ───────────────────────────────────────────────

    @Test fun hpAboveStop_belowStopPct_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_STOP_PCT to 90)
        assertFalse(RecoveryManager.hpAboveStopThreshold(state(70, 100), p))
    }

    @Test fun hpAboveStop_atStopPct_returnsTrue() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_STOP_PCT to 90)
        assertTrue(RecoveryManager.hpAboveStopThreshold(state(90, 100), p))
    }

    @Test fun hpAboveStop_aboveStopPct_returnsTrue() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_STOP_PCT to 90)
        assertTrue(RecoveryManager.hpAboveStopThreshold(state(95, 100), p))
    }

    @Test fun hpAboveStop_zeroMaxHp_returnsTrue() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true, Preferences.HP_RECOVERY_STOP_PCT to 90)
        assertTrue(RecoveryManager.hpAboveStopThreshold(state(0, 0), p))
    }

    @Test fun hpAboveStop_defaultStopPctIs90() {
        val p = prefs(Preferences.AUTO_RECOVER_HP to true)
        assertFalse(RecoveryManager.hpAboveStopThreshold(state(89, 100), p))
        assertTrue(RecoveryManager.hpAboveStopThreshold(state(90, 100), p))
    }

    @Test fun mpAboveStop_belowStopPct_returnsFalse() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_STOP_PCT to 80)
        assertFalse(RecoveryManager.mpAboveStopThreshold(state(0, 0, 50, 100), p))
    }

    @Test fun mpAboveStop_atStopPct_returnsTrue() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_STOP_PCT to 80)
        assertTrue(RecoveryManager.mpAboveStopThreshold(state(0, 0, 80, 100), p))
    }

    @Test fun mpAboveStop_zeroMaxMp_returnsTrue() {
        val p = prefs(Preferences.AUTO_RECOVER_MP to true, Preferences.MP_RECOVERY_STOP_PCT to 80)
        assertTrue(RecoveryManager.mpAboveStopThreshold(state(0, 0, 0, 0), p))
    }
}
