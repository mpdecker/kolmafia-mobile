package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext

class GameRuntimeLibraryAshP60Test {

    @Test
    fun mosquito_rawIntegerEqualsDb() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(16, CombatAdjustment.monsterRawAttack(mosquito))
        assertEquals(14, CombatAdjustment.monsterRawDefense(mosquito))
        assertEquals(18, CombatAdjustment.monsterRawHp(mosquito))
        assertEquals(20, CombatAdjustment.monsterRawInitiative(mosquito))
        // Raw ignores ML — same as effective with ml=0 for integers
        assertEquals(16, CombatAdjustment.monsterAttack(mosquito, ml = 0))
        assertEquals(26, CombatAdjustment.monsterAttack(mosquito, ml = 10))
        assertEquals(16, CombatAdjustment.monsterRawAttack(mosquito))
    }

    @Test
    fun sourceAgent_rawExpressionNoOuterMl() = runBlocking {
        MonsterDatabase.load()
        val agent = MonsterDatabase.getByName("Source Agent")!!
        val ctx0 = ExpressionContext(
            monsterLevel = 0,
            prefLookup = { if (it == "sourceAgentsDefeated") "0" else "" },
        )
        // Atk: [30+30*pref+ML] → 30
        assertEquals(30, CombatAdjustment.monsterRawAttack(agent, ctx0))
        val ctxMl = ExpressionContext(
            monsterLevel = 10,
            prefLookup = { if (it == "sourceAgentsDefeated") "0" else "" },
        )
        // ML inside expression counts; no outer ML add
        assertEquals(40, CombatAdjustment.monsterRawAttack(agent, ctxMl))
        assertEquals(40, CombatAdjustment.monsterAttack(agent, ml = 99, ctxMl))
    }

    @Test
    fun amokPutty_rawScaleNoMl() = runBlocking {
        MonsterDatabase.load()
        val putty = MonsterDatabase.getByName("amok putty")!!
        assertTrue(putty.isScaling)
        assertFalse(putty.hasAttack)
        val ctx = ExpressionContext(buffedMoxie = 50, buffedMuscle = 50)
        // Scale:1 Cap:89 Floor:6 → Atk 51
        assertEquals(51, CombatAdjustment.monsterRawAttack(putty, ctx))
        assertEquals(51, CombatAdjustment.monsterRawDefense(putty, ctx))
        // HP: floor(51 * 0.75) = 38 (floor applied before ×0.75)
        assertEquals(38, CombatAdjustment.monsterRawHp(putty, ctx))
        // Effective with ML=10 differs from raw
        assertEquals(61, CombatAdjustment.monsterAttack(putty, ml = 10, ctx))
        assertEquals(51, CombatAdjustment.monsterRawAttack(putty, ctx))
    }

    @Test
    fun missingAtk_nonScaleReturnsNegOne() = runBlocking {
        MonsterDatabase.load()
        val darkness = MonsterDatabase.getByName("the darkness (blind)")!!
        assertFalse(darkness.hasAttack)
        assertFalse(darkness.isScaling)
        assertEquals(-1, CombatAdjustment.monsterRawAttack(darkness))
        assertEquals(-1, CombatAdjustment.monsterRawDefense(darkness))
        assertEquals(-1, CombatAdjustment.monsterRawHp(darkness))
    }

    @Test
    fun missingInit_returnsZero() = runBlocking {
        MonsterDatabase.load()
        val bastard = MonsterDatabase.getByName("crazy bastard")!!
        assertFalse(bastard.hasInitiative)
        assertEquals(0, CombatAdjustment.monsterRawInitiative(bastard))
    }

    @Test
    fun ash_brackets_mosquitoAndPutty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(buffedmox = "50", mox = "50", buffedmus = "50", mus = "50"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "16",
            outputLib(lib, """print(to_monster("huge mosquito")["raw_attack"]);""").trim(),
        )
        assertEquals(
            "18",
            outputLib(lib, """print(to_monster("huge mosquito")["raw_hp"]);""").trim(),
        )
        assertEquals(
            "51",
            outputLib(lib, """print(to_monster("amok putty")["raw_attack"]);""").trim(),
        )
        assertEquals(
            "38",
            outputLib(lib, """print(to_monster("amok putty")["raw_hp"]);""").trim(),
        )
        assertEquals(
            "-1",
            outputLib(lib, """print(to_monster("the darkness (blind)")["raw_attack"]);""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }
}
