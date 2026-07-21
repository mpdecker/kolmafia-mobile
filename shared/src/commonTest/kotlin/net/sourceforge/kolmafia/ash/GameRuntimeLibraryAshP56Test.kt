package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP56Test {

    @Test
    fun zeroReduce_baselinesUnchanged() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val putty = MonsterDatabase.getByName("amok putty")!!
        val agent = MonsterDatabase.getByName("Source Agent")!!
        val ctx = ExpressionContext(buffedMuscle = 50, buffedMoxie = 50)
        assertEquals(14, CombatAdjustment.monsterDefense(mosquito, 0))
        assertEquals(51, CombatAdjustment.monsterDefense(putty, 0, ctx))
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val agentCtx = ExpressionContext(prefLookup = { prefs.getString(it, "") })
        assertEquals(30, CombatAdjustment.monsterDefense(agent, 0, agentCtx))
    }

    @Test
    fun reduce_integerScaleAndExpressionDefense() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        // 14 * 0.9 = 12.6 → 12; 14 * 0.8 = 11.2 → 11
        assertEquals(12, CombatAdjustment.monsterDefense(mosquito, 0, reduceEnemyDefensePercent = 10.0))
        assertEquals(11, CombatAdjustment.monsterDefense(mosquito, 0, reduceEnemyDefensePercent = 20.0))

        val putty = MonsterDatabase.getByName("amok putty")!!
        val ctx = ExpressionContext(buffedMuscle = 50)
        // Scale Def 51 * 0.9 → 45
        assertEquals(45, CombatAdjustment.monsterDefense(putty, 0, ctx, 10.0))

        val agent = MonsterDatabase.getByName("Source Agent")!!
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val agentCtx = ExpressionContext(prefLookup = { prefs.getString(it, "") })
        // Expr Def 30 * 0.9 → 27
        assertEquals(27, CombatAdjustment.monsterDefense(agent, 0, agentCtx, 10.0))
    }

    @Test
    fun ash_equipSharpshooterHat_reducesDefense() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        val libBare = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "14",
            outputLib(libBare, """print(monster_defense(to_monster("huge mosquito")));""").trim(),
        )
        char.updateEquipment(EquipmentSlot.HAT, "sharpshooter's hat")
        val libHat = GameRuntimeLibrary(gameDatabase = db, character = char)
        // 5% reduce → floor(14 * 0.95) = floor(13.3) = 13
        assertEquals(
            "13",
            outputLib(libHat, """print(monster_defense(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun willUsuallyMiss_flipsWhenDefenseReduced() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        // hitStat == def 14 → hitPercent 50 → miss
        assertTrue(CombatAdjustment.willUsuallyMiss(mosquito, hitStat = 14, ml = 0))
        // 20% → def 11; hitStat 14 → hitPercent > 50 → not miss
        assertFalse(
            CombatAdjustment.willUsuallyMiss(
                mosquito,
                hitStat = 14,
                ml = 0,
                reduceEnemyDefensePercent = 20.0,
            ),
        )
    }

    @Test
    fun ash_sharpshooterHat_willUsuallyMissResponds() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(buffedmus = "14", buffedmox = "14"))
        }
        val libBare = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        assertEquals(
            "true",
            outputLib(libBare, """print(to_string(will_usually_miss()));""").trim(),
        )
        // 5% → def 13; mus hit 14 → hitPercent > 50 → no miss
        char.updateEquipment(EquipmentSlot.HAT, "sharpshooter's hat")
        val libHat = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        assertEquals(
            "false",
            outputLib(libHat, """print(to_string(will_usually_miss()));""").trim(),
        )
    }

    @Test
    fun revision_isPhase98() {
        assertEquals("phase126", GameRuntimeLibrary.REVISION)
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
    }
}
