package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP54Test {

    @Test
    fun sausageGoblin_scalePrefEvaluates() = runBlocking {
        MonsterDatabase.load()
        val goblin = MonsterDatabase.getByName("sausage goblin")!!
        assertTrue(goblin.isScaling)
        assertNotNull(goblin.scaleExpression)
        val ctx0 = ExpressionContext(
            buffedMoxie = 50,
            prefLookup = { if (it == "_sausageFights") "0" else "" },
        )
        // Scale 1+2*0=1 → min(51, 10000)=51
        assertEquals(1, CombatAdjustment.resolveScaleParams(goblin, ctx0).scale)
        assertEquals(51, CombatAdjustment.monsterAttack(goblin, ml = 0, ctx0))

        val ctx3 = ExpressionContext(
            buffedMoxie = 50,
            prefLookup = { if (it == "_sausageFights") "3" else "" },
        )
        assertEquals(7, CombatAdjustment.resolveScaleParams(goblin, ctx3).scale)
        assertEquals(57, CombatAdjustment.monsterAttack(goblin, ml = 0, ctx3))
    }

    @Test
    fun drunkCowpoke_scaleAndFloorExprs() = runBlocking {
        MonsterDatabase.load()
        val cowpoke = MonsterDatabase.getByName("drunk cowpoke")!!
        assertNotNull(cowpoke.scaleExpression)
        assertNotNull(cowpoke.floorExpression)

        val easy = ExpressionContext(
            buffedMoxie = 50,
            prefLookup = { if (it == "lttQuestDifficulty") "0" else "" },
        )
        // Scale max(5,-25)=5; Floor 0 → Atk 55
        assertEquals(5, CombatAdjustment.resolveScaleParams(cowpoke, easy).scale)
        assertEquals(0, CombatAdjustment.resolveScaleParams(cowpoke, easy).floor)
        assertEquals(55, CombatAdjustment.monsterAttack(cowpoke, ml = 0, easy))

        val hard = ExpressionContext(
            buffedMoxie = 50,
            prefLookup = { if (it == "lttQuestDifficulty") "2" else "" },
        )
        // Scale 25; Floor 120 → Atk max(75, 120)=120
        assertEquals(25, CombatAdjustment.resolveScaleParams(cowpoke, hard).scale)
        assertEquals(120, CombatAdjustment.resolveScaleParams(cowpoke, hard).floor)
        assertEquals(120, CombatAdjustment.monsterAttack(cowpoke, ml = 0, hard))
    }

    @Test
    fun biker_equippedFlipsScale() = runBlocking {
        val db = GameDatabase()
        db.load()
        val biker = MonsterDatabase.getByName("biker")!!
        assertNotNull(biker.scaleExpression)

        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(buffedmox = "50", mox = "50"))
        }
        val libBare = GameRuntimeLibrary(gameDatabase = db, character = char)
        // Scale 20+0 → Atk min(70, 20000)=70
        assertEquals(
            "70",
            outputLib(libBare, """print(monster_attack(to_monster("biker")));""").trim(),
        )

        char.updateEquipment(EquipmentSlot.SHIRT, "PARTY HARD T-shirt")
        val libHard = GameRuntimeLibrary(gameDatabase = db, character = char)
        // Scale 20+100=120 → Atk 170
        assertEquals(
            "170",
            outputLib(libHard, """print(monster_attack(to_monster("biker")));""").trim(),
        )
    }

    @Test
    fun amokPutty_numericScaleUnchanged() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(buffedmox = "50", mox = "50"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "51",
            outputLib(lib, """print(monster_attack(to_monster("amok putty")));""").trim(),
        )
    }

    @Test
    fun sourceAgent_expressionAtkUnchanged() = runBlocking {
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
    fun revision_isPhase96() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
    }
}
