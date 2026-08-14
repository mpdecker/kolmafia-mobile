package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP53Test {

    @Test
    fun amokPutty_scaleAttackWithBuffedMoxie() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(buffedmox = "50", mox = "50"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        // Scale: 1 Cap: 89 Floor: 6; min(89, 50+1)+0 = 51
        assertEquals(
            "51",
            outputLib(lib, """print(monster_attack(to_monster("amok putty")));""").trim(),
        )
    }

    @Test
    fun amokPutty_scaleAttackAddsMl() = runBlocking {
        MonsterDatabase.load()
        val putty = MonsterDatabase.getByName("amok putty")!!
        assertTrue(putty.isScaling)
        assertEquals(false, putty.hasAttack)
        val ctx = ExpressionContext(buffedMoxie = 50)
        assertEquals(51, CombatAdjustment.monsterAttack(putty, ml = 0, ctx))
        assertEquals(61, CombatAdjustment.monsterAttack(putty, ml = 10, ctx))
    }

    @Test
    fun amokPutty_scaleDefenseAndHp() = runBlocking {
        MonsterDatabase.load()
        val putty = MonsterDatabase.getByName("amok putty")!!
        val ctx = ExpressionContext(buffedMuscle = 50, buffedMoxie = 50)
        // Def: same as Atk with muscle → 51
        assertEquals(51, CombatAdjustment.monsterDefense(putty, ml = 0, ctx))
        // HP: floor(51 * 0.75) = 38
        assertEquals(38, CombatAdjustment.monsterHp(putty, ml = 0, ctx))
    }

    @Test
    fun fullLengthMirror_capQuestionBecomesDefault() = runBlocking {
        MonsterDatabase.load()
        val mirror = MonsterDatabase.getByName("full-length mirror")!!
        assertTrue(mirror.isScaling)
        assertEquals(MonsterDefinition.DEFAULT_CAP, mirror.cap)
        assertEquals(MonsterDefinition.DEFAULT_FLOOR, mirror.floor)
        val ctx = ExpressionContext(buffedMoxie = 50)
        // Scale: 0 Cap: 10000 → min(10000, 50+0) = 50
        assertEquals(50, CombatAdjustment.monsterAttack(mirror, ml = 0, ctx))
    }

    @Test
    fun numericAtk_mosquitoUnchanged() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "16",
            outputLib(lib, """print(monster_attack(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun expressionAtk_sourceAgentUnchanged() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "30",
            outputLib(lib, """print(monster_attack(to_monster("Source Agent")));""").trim(),
        )
    }

    @Test
    fun expectedDamage_scaleMonsterUsesScaledAttack() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(classId = "5", buffedmox = "50", mox = "50"),
            )
        }
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "amok putty")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        val putty = MonsterDatabase.getByName("amok putty")!!
        val mods = lib.buildCurrentModifiers()
        val ctx = lib.buildMonsterExpressionContext()
        val expected = CombatAdjustment.expectedDamage(
            putty,
            char.state.value,
            mods,
            ml = 0,
            expressionContext = ctx,
        )
        assertEquals(
            expected.toString(),
            outputLib(lib, """print(expected_damage(to_monster("amok putty")));""").trim(),
        )
        assertTrue(expected > 0)
        // Scaled atk 51 with moxie 50: base = max(0,1) + 51/4 = 13; *1.1 DA → 15
        assertTrue(expected >= 10)
        // Must not use raw attack 0
        val rawZero = CombatAdjustment.expectedDamage(
            putty.copy(isScaling = false, hasAttack = true, attack = 0),
            char.state.value,
            mods,
        )
        assertEquals(0, rawZero)
        assertTrue(expected > rawZero)
    }

    @Test
    fun revision_isPhase95() {
        assertEquals("phase479", GameRuntimeLibrary.REVISION)
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
    }
}
