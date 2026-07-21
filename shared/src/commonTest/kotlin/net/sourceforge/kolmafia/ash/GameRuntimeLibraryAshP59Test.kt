package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext

class GameRuntimeLibraryAshP59Test {

    @Test
    fun mlBoost_mosquitoPhysZero() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(0, CombatAdjustment.monsterPhysicalResistance(mosquito, ml = 0))
        // floor(100/2.5)=40
        assertEquals(40, CombatAdjustment.mlPhysicalResistanceBoost(mosquito, 100))
        assertEquals(40, CombatAdjustment.monsterPhysicalResistance(mosquito, ml = 100))
        // floor(200/2.5)=80 → cap 50
        assertEquals(50, CombatAdjustment.mlPhysicalResistanceBoost(mosquito, 200))
        assertEquals(50, CombatAdjustment.monsterPhysicalResistance(mosquito, ml = 200))
    }

    @Test
    fun caveBars_mlMultScalesBoost() = runBlocking {
        MonsterDatabase.load()
        val bars = MonsterDatabase.getByName("clan of cave bars")!!
        // ML=10 × MLMult:5 → effective 50 → floor(50/2.5)=20
        assertEquals(20, CombatAdjustment.mlPhysicalResistanceBoost(bars, 10))
        assertEquals(20, CombatAdjustment.monsterPhysicalResistance(bars, ml = 10))
        // Without MLMult, same global ML would only boost floor(10/2.5)=4
        val putty = MonsterDatabase.getByName("amok putty")!!
        assertEquals(4, CombatAdjustment.mlPhysicalResistanceBoost(putty, 10))
    }

    @Test
    fun basePhys_mergeRules() = runBlocking {
        MonsterDatabase.load()
        val aps = MonsterDatabase.getByName("ancient protector spirit")!!
        // Phys:100; boost 40 ≤ 100 → keep 100
        assertEquals(100, CombatAdjustment.monsterPhysicalResistance(aps, ml = 100))
        // Phys:100; boost 50 ≤ 100 → keep 100
        assertEquals(100, CombatAdjustment.monsterPhysicalResistance(aps, ml = 200))

        val oil = MonsterDatabase.getByName("oil baron")!!
        // Phys:50; boost 40 ≤ 50 → keep 50
        assertEquals(50, CombatAdjustment.monsterPhysicalResistance(oil, ml = 100))
        // Phys:50; boost 50 ≤ 50 → keep 50
        assertEquals(50, CombatAdjustment.monsterPhysicalResistance(oil, ml = 200))
    }

    @Test
    fun scaleNegativeMl_boostClampedToZero() = runBlocking {
        MonsterDatabase.load()
        val putty = MonsterDatabase.getByName("amok putty")!!
        assertTrue(putty.isScaling)
        assertEquals(0, CombatAdjustment.mlPhysicalResistanceBoost(putty, -100))
        assertEquals(0, CombatAdjustment.monsterPhysicalResistance(putty, ml = -100))
    }

    @Test
    fun ash_bracket_mlZeroRegression() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "100",
            outputLib(lib, """print(to_monster("ancient protector spirit")["physical_resistance"]);""").trim(),
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_monster("huge mosquito")["physical_resistance"]);""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase121", GameRuntimeLibrary.REVISION)
    }
}
