package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext

class GameRuntimeLibraryAshP61Test {

    @Test
    fun mosquito_mlZero_baseEqualsRaw() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(16, CombatAdjustment.monsterAttack(mosquito, ml = 0))
        assertEquals(16, CombatAdjustment.monsterRawAttack(mosquito))
        assertEquals(14, CombatAdjustment.monsterDefense(mosquito, ml = 0))
        assertEquals(14, CombatAdjustment.monsterRawDefense(mosquito))
        assertEquals(18, CombatAdjustment.monsterHp(mosquito, ml = 0))
        assertEquals(18, CombatAdjustment.monsterRawHp(mosquito))
        assertEquals(20, CombatAdjustment.monsterInitiativeWithMl(mosquito, ml = 0))
        assertEquals(20, CombatAdjustment.monsterRawInitiative(mosquito))
    }

    @Test
    fun mosquito_mlTen_baseDivergesFromRaw() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(26, CombatAdjustment.monsterAttack(mosquito, ml = 10))
        assertEquals(16, CombatAdjustment.monsterRawAttack(mosquito))
        assertEquals(24, CombatAdjustment.monsterDefense(mosquito, ml = 10))
        assertEquals(14, CombatAdjustment.monsterRawDefense(mosquito))
        assertEquals(28, CombatAdjustment.monsterHp(mosquito, ml = 10))
        assertEquals(18, CombatAdjustment.monsterRawHp(mosquito))
    }

    @Test
    fun amokPutty_scaleMl_baseVsRaw() = runBlocking {
        MonsterDatabase.load()
        val putty = MonsterDatabase.getByName("amok putty")!!
        val ctx = ExpressionContext(buffedMoxie = 50, buffedMuscle = 50)
        assertEquals(61, CombatAdjustment.monsterAttack(putty, ml = 10, ctx))
        assertEquals(51, CombatAdjustment.monsterRawAttack(putty, ctx))
        assertEquals(61, CombatAdjustment.monsterDefense(putty, ml = 10, ctx))
        assertEquals(51, CombatAdjustment.monsterRawDefense(putty, ctx))
    }

    @Test
    fun initiative_initPenaltyAboveMl20() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        // initPenalty(30) = 10 → base_initiative 30
        assertEquals(30, CombatAdjustment.monsterInitiativeWithMl(mosquito, ml = 30))
        assertEquals(20, CombatAdjustment.monsterRawInitiative(mosquito))
        assertEquals(10, CombatAdjustment.initPenalty(30))
    }

    @Test
    fun defense_reduceEnemyDefense() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        // Def 14, 50% reduce → floor(max(1, 14*0.5)) = 7
        assertEquals(
            7,
            CombatAdjustment.monsterDefense(mosquito, ml = 0, reduceEnemyDefensePercent = 50.0),
        )
        assertEquals(14, CombatAdjustment.monsterRawDefense(mosquito))
    }

    @Test
    fun ash_brackets_mlZeroRegression() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "16",
            outputLib(lib, """print(to_monster("huge mosquito")["base_attack"]);""").trim(),
        )
        assertEquals(
            "18",
            outputLib(lib, """print(to_monster("huge mosquito")["base_hp"]);""").trim(),
        )
        assertEquals(
            "20",
            outputLib(lib, """print(to_monster("huge mosquito")["base_initiative"]);""").trim(),
        )
        assertEquals(
            "16",
            outputLib(lib, """print(to_monster("huge mosquito")["raw_attack"]);""").trim(),
        )
    }

    @Test
    fun ash_brackets_puttyScaleNoMl() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(buffedmox = "50", mox = "50", buffedmus = "50", mus = "50"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "51",
            outputLib(lib, """print(to_monster("amok putty")["base_attack"]);""").trim(),
        )
        assertEquals(
            "51",
            outputLib(lib, """print(to_monster("amok putty")["raw_attack"]);""").trim(),
        )
    }

    @Test
    fun entityFields_explicitMlAndReduce() = runBlocking {
        val db = GameDatabase()
        db.load()
        val ctx = ExpressionContext()
        assertEquals(
            26L,
            MonsterEntityFields.resolve(
                "huge mosquito",
                "base_attack",
                db,
                expressionContext = ctx,
                ml = 10,
            ).toLong(),
        )
        assertEquals(
            16L,
            MonsterEntityFields.resolve(
                "huge mosquito",
                "raw_attack",
                db,
                expressionContext = ctx,
                ml = 10,
            ).toLong(),
        )
        assertEquals(
            7L,
            MonsterEntityFields.resolve(
                "huge mosquito",
                "base_defense",
                db,
                expressionContext = ctx,
                ml = 0,
                reduceEnemyDefensePercent = 50.0,
            ).toLong(),
        )
        assertEquals(
            30L,
            MonsterEntityFields.resolve(
                "huge mosquito",
                "base_initiative",
                db,
                expressionContext = ctx,
                ml = 30,
            ).toLong(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase200", GameRuntimeLibrary.REVISION)
    }
}
