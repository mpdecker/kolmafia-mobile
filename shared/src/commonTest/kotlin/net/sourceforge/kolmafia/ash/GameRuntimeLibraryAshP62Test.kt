package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP62Test {

    @Test
    fun mosquito_mlZero_matchesRaw() = runBlocking {
        MonsterDatabase.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(20, CombatAdjustment.monsterInitiativeWithMl(mosquito, ml = 0))
        assertEquals(20, CombatAdjustment.monsterRawInitiative(mosquito))
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "20",
            outputLib(lib, """print(monster_initiative(to_monster("huge mosquito")));""").trim(),
        )
        assertEquals(
            "20",
            outputLib(lib, """print(to_monster("huge mosquito")["base_initiative"]);""").trim(),
        )
        assertEquals(
            "20",
            outputLib(lib, """print(to_monster("huge mosquito")["raw_initiative"]);""").trim(),
        )
    }

    @Test
    fun mosquito_ml30_addsInitPenalty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        // initPenalty(30)=10 → 20+10=30
        assertEquals(30, CombatAdjustment.monsterInitiativeWithMl(mosquito, ml = 30))
        assertEquals(20, CombatAdjustment.monsterRawInitiative(mosquito))
        assertEquals(
            30L,
            MonsterEntityFields.resolve(
                "huge mosquito",
                "base_initiative",
                db,
                ml = 30,
            ).toLong(),
        )
    }

    @Test
    fun sentinel_initNeg10000_skipsPenalty() = runBlocking {
        MonsterDatabase.load()
        val bars = MonsterDatabase.getByName("clan of cave bars")!!
        assertEquals(-10000, CombatAdjustment.monsterRawInitiative(bars))
        assertEquals(-10000, CombatAdjustment.monsterInitiativeWithMl(bars, ml = 100))
    }

    @Test
    fun ash_raincore_matchesBaseInitiativeBracket() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_LOCATION, "Dreadsylvanian Village")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(path = AscensionPath.HEAVY_RAINS.apiName),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        // Village waterLevel 4 → ML += 40; initPenalty(40)=20 → 40
        val expected = CombatAdjustment.monsterInitiativeWithMl(
            MonsterDatabase.getByName("huge mosquito")!!,
            ml = 40,
        ).toString()
        assertEquals(
            expected,
            outputLib(lib, """print(monster_initiative(to_monster("huge mosquito")));""").trim(),
        )
        assertEquals(
            expected,
            outputLib(lib, """print(to_monster("huge mosquito")["base_initiative"]);""").trim(),
        )
    }

    @Test
    fun noneMonster_returnsZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("0", outputLib(lib, """print(monster_initiative(to_monster("none")));""").trim())
    }

    @Test
    fun revision_phase120() {
        assertEquals("phase370", GameRuntimeLibrary.REVISION)
    }
}
