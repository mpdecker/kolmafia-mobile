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

class GameRuntimeLibraryAshP41Test {

    @Test
    fun monsterStats_oneArg_matchDefinitionAtZeroMl() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            mosquito.attack.toString(),
            outputLib(lib, """print(monster_attack(to_monster("huge mosquito")));""").trim(),
        )
        assertEquals(
            mosquito.defense.toString(),
            outputLib(lib, """print(monster_defense(to_monster("huge mosquito")));""").trim(),
        )
        assertEquals(
            mosquito.hp.toString(),
            outputLib(lib, """print(monster_hp(to_monster("huge mosquito")));""").trim(),
        )
        assertEquals(
            mosquito.initiative.toString(),
            outputLib(lib, """print(monster_initiative(to_monster("huge mosquito")));""").trim(),
        )
        assertEquals(
            mosquito.phylum,
            outputLib(lib, """print(monster_phylum(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun monsterStats_increaseWithRaincoreMl() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_LOCATION, "Dreadsylvanian Village")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(path = AscensionPath.HEAVY_RAINS.apiName),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        // Village waterLevel 4 → ML += 40
        val ml = 40
        assertEquals(
            CombatAdjustment.monsterAttack(mosquito, ml).toString(),
            outputLib(lib, """print(monster_attack(to_monster("huge mosquito")));""").trim(),
        )
        assertEquals(
            CombatAdjustment.monsterDefense(mosquito, ml).toString(),
            outputLib(lib, """print(monster_defense(to_monster("huge mosquito")));""").trim(),
        )
        assertEquals(
            CombatAdjustment.monsterHp(mosquito, ml).toString(),
            outputLib(lib, """print(monster_hp(to_monster("huge mosquito")));""").trim(),
        )
        // Initiative stays base
        assertEquals(
            mosquito.initiative.toString(),
            outputLib(lib, """print(monster_initiative(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun monsterStats_zeroArg_readsLastMonster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(mosquito.attack.toString(), outputLib(lib, """print(monster_attack());""").trim())
        assertEquals(mosquito.defense.toString(), outputLib(lib, """print(monster_defense());""").trim())
        assertEquals(mosquito.hp.toString(), outputLib(lib, """print(monster_hp());""").trim())
        assertEquals(
            mosquito.initiative.toString(),
            outputLib(lib, """print(monster_initiative());""").trim(),
        )
        assertEquals(mosquito.phylum, outputLib(lib, """print(monster_phylum());""").trim())
    }

    @Test
    fun monsterStats_noneMonster_returnsZeros() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("0", outputLib(lib, """print(monster_attack(to_monster("none")));""").trim())
        assertEquals("0", outputLib(lib, """print(monster_defense(to_monster("none")));""").trim())
        assertEquals("0", outputLib(lib, """print(monster_hp(to_monster("none")));""").trim())
        assertEquals("0", outputLib(lib, """print(monster_initiative(to_monster("none")));""").trim())
        assertEquals("", outputLib(lib, """print(monster_phylum(to_monster("none")));""").trim())
    }
}
