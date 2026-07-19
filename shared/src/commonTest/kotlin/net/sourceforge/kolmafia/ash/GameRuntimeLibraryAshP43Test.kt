package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP43Test {

    @Test
    fun jumpChance_threeArg_matchesHelper() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(classId = "5", mox = "30"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        val expected = CombatAdjustment.jumpChance(
            monster = mosquito,
            initBonus = 0,
            initMl = 0,
            attackMl = 0,
            baseMainstat = 30,
        )
        assertEquals(
            expected.toString(),
            outputLib(
                lib,
                """print(jump_chance(to_monster("huge mosquito"), 0, 0));""",
            ).trim(),
        )
        // 100 - 20 + 0 + max(0, 30-16) = 94
        assertEquals(94, expected)
    }

    @Test
    fun jumpChance_mlAffectsInitPenalty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val atZero = CombatAdjustment.jumpChance(mosquito, 0, 0, 0, 0)
        val atForty = CombatAdjustment.jumpChance(mosquito, 0, 40, 0, 0)
        assertEquals(atZero.toString(), outputLib(lib, """print(jump_chance(to_monster("huge mosquito"), 0, 0));""").trim())
        assertEquals(atForty.toString(), outputLib(lib, """print(jump_chance(to_monster("huge mosquito"), 0, 40));""").trim())
        assertEquals(80, atZero)
        assertEquals(60, atForty)
    }

    @Test
    fun jumpChance_zeroArg_readsLastMonster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        val expected = CombatAdjustment.jumpChance(mosquito, 0, 0, 0, 0)
        assertEquals(expected.toString(), outputLib(lib, """print(jump_chance());""").trim())
    }

    @Test
    fun jumpChance_noneMonster_returnsZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("0", outputLib(lib, """print(jump_chance(to_monster("none")));""").trim())
    }
}
