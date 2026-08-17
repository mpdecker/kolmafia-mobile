package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
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
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP58Test {

    @Test
    fun caveBars_mlMultFive() = runBlocking {
        MonsterDatabase.load()
        val bars = MonsterDatabase.getByName("clan of cave bars")!!
        assertTrue(bars.hasMlMult)
        assertEquals(5, bars.mlMult)
        assertEquals(null, bars.mlMultExpression)
        val ctx = ExpressionContext(buffedMoxie = 50, buffedMuscle = 50)
        // Scale:20 Cap:200 Floor:30; base min(70,200)=70
        assertEquals(70, CombatAdjustment.monsterAttack(bars, ml = 0, ctx))
        // ML=10 → effective 50 → Atk 120
        assertEquals(120, CombatAdjustment.monsterAttack(bars, ml = 10, ctx))
        assertEquals(120, CombatAdjustment.monsterDefense(bars, ml = 10, ctx))
        // HP: floor((70+50)*0.75)=90
        assertEquals(90, CombatAdjustment.monsterHp(bars, ml = 10, ctx))
        assertEquals(50, CombatAdjustment.effectiveMonsterLevel(bars, 10, ctx))
    }

    @Test
    fun yamGolem_mlMultZeroIgnoresMl() = runBlocking {
        MonsterDatabase.load()
        val yam = MonsterDatabase.getByName("Hammered Yam Golem")!!
        assertTrue(yam.hasMlMult)
        assertEquals(0, yam.mlMult)
        val ctx = ExpressionContext(buffedMoxie = 50, buffedMuscle = 50)
        // Scale:-3 → base 47; MLMult:0 → ML ignored
        assertEquals(47, CombatAdjustment.monsterAttack(yam, ml = 0, ctx))
        assertEquals(47, CombatAdjustment.monsterAttack(yam, ml = 10, ctx))
        assertEquals(0, CombatAdjustment.effectiveMonsterLevel(yam, 10, ctx))
    }

    @Test
    fun dinsey_mlMultExpressionPref() = runBlocking {
        MonsterDatabase.load()
        val bear = MonsterDatabase.getByName("C<i>bzzt</i>er the Grisly Bear")!!
        assertTrue(bear.hasMlMult)
        assertEquals("3+2*pref(dinseyAudienceEngagement)", bear.mlMultExpression)

        val ctx0 = ExpressionContext(
            buffedMoxie = 50,
            buffedMuscle = 50,
            prefLookup = { if (it == "dinseyAudienceEngagement") "0" else "" },
        )
        // Scale 5, MLMult 3 → effective ML 30; Atk min(55,11111)+30=85, floor 100 → 100
        assertEquals(30, CombatAdjustment.effectiveMonsterLevel(bear, 10, ctx0))
        assertEquals(100, CombatAdjustment.monsterAttack(bear, ml = 10, ctx0))

        val ctx1 = ExpressionContext(
            buffedMoxie = 50,
            buffedMuscle = 50,
            prefLookup = { if (it == "dinseyAudienceEngagement") "1" else "" },
        )
        // Scale 25, MLMult 5 → effective ML 50; Atk 75+50=125
        assertEquals(50, CombatAdjustment.effectiveMonsterLevel(bear, 10, ctx1))
        assertEquals(125, CombatAdjustment.monsterAttack(bear, ml = 10, ctx1))
    }

    @Test
    fun unsetMlMult_identity() = runBlocking {
        MonsterDatabase.load()
        val putty = MonsterDatabase.getByName("amok putty")!!
        assertFalse(putty.hasMlMult)
        val ctx = ExpressionContext(buffedMoxie = 50)
        assertEquals(10, CombatAdjustment.effectiveMonsterLevel(putty, 10, ctx))
        assertEquals(61, CombatAdjustment.monsterAttack(putty, ml = 10, ctx))
    }

    @Test
    fun ash_caveBars_monsterAttack() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        // Inject ML via a known modifier path: set numeric_modifier through char is hard;
        // CombatAdjustment path is covered above — smoke ASH with ML=0.
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(buffedmox = "50", mox = "50", buffedmus = "50", mus = "50"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        assertEquals(
            "70",
            outputLib(lib, """print(monster_attack(to_monster("clan of cave bars")));""").trim(),
        )
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase550", GameRuntimeLibrary.REVISION)
    }
}
