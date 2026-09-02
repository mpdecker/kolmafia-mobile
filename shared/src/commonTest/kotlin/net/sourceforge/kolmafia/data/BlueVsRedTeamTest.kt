package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.MonsterEntityFields
import net.sourceforge.kolmafia.ash.outputLib

class BlueVsRedTeamTest {

    @Test
    fun from_mapsTeamNamesCaseInsensitive() {
        assertEquals(BlueVsRedTeam.BLUE, BlueVsRedTeam.from("blue"))
        assertEquals(BlueVsRedTeam.RED, BlueVsRedTeam.from("RED"))
        assertEquals(BlueVsRedTeam.ENEMY, BlueVsRedTeam.from("Enemy"))
        assertEquals(BlueVsRedTeam.UNKNOWN, BlueVsRedTeam.from("unknown"))
        assertEquals(BlueVsRedTeam.UNKNOWN, BlueVsRedTeam.from("not-a-team"))
    }

    @Test
    fun teamName_matchesDesktopStrings() {
        assertEquals("blue", BlueVsRedTeam.BLUE.teamName)
        assertEquals("red", BlueVsRedTeam.RED.teamName)
        assertEquals("enemy", BlueVsRedTeam.ENEMY.teamName)
        assertEquals("unknown", BlueVsRedTeam.UNKNOWN.teamName)
    }

    @Test
    fun definitionDefault_isUnknown() {
        val monster = MonsterDefinition(
            name = "unaffiliated",
            id = 1,
            image = "",
            attack = 1,
            defense = 1,
            hp = 1,
            initiative = 0,
            meatDrop = 0,
            phylum = "dude",
            isBoss = false,
            isGhost = false,
            isLucky = false,
            isScaling = false,
            scale = 0,
            cap = 0,
            floor = 0,
            drops = emptyList(),
        )
        assertEquals(BlueVsRedTeam.UNKNOWN, monster.blueVsRedTeam)
    }

    @Test
    fun parse_haxxorIsRed() = runBlocking {
        MonsterDatabase.load()
        assertEquals(BlueVsRedTeam.RED, MonsterDatabase.getByName("1335 HaXx0r")?.blueVsRedTeam)
    }

    @Test
    fun parse_ancestralPortraitIsBlue() = runBlocking {
        MonsterDatabase.load()
        assertEquals(
            BlueVsRedTeam.BLUE,
            MonsterDatabase.getByName("ancestral Spookyraven portrait")?.blueVsRedTeam,
        )
    }

    @Test
    fun parse_baabaaburanIsEnemy() = runBlocking {
        MonsterDatabase.load()
        assertEquals(BlueVsRedTeam.ENEMY, MonsterDatabase.getByName("Baa'baa'bu'ran")?.blueVsRedTeam)
    }

    @Test
    fun parse_absentBvRIsUnknown() = runBlocking {
        MonsterDatabase.load()
        assertEquals(BlueVsRedTeam.UNKNOWN, MonsterDatabase.getByName("huge mosquito")?.blueVsRedTeam)
    }

    @Test
    fun ash_haxxorBracketReturnsRed() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "red",
            outputLib(lib, """print(to_monster("1335 HaXx0r")["blue_vs_red_team"]);""").trim(),
        )
        assertEquals(
            "red",
            MonsterEntityFields.resolve("1335 HaXx0r", "blue_vs_red_team", db).toString(),
        )
    }

    @Test
    fun ash_portraitAndAbsentAndUnknownMonster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "blue",
            outputLib(
                lib,
                """print(to_monster("ancestral Spookyraven portrait")["blue_vs_red_team"]);""",
            ).trim(),
        )
        assertEquals(
            "unknown",
            outputLib(lib, """print(to_monster("huge mosquito")["blue_vs_red_team"]);""").trim(),
        )
        assertEquals(
            "",
            outputLib(lib, """print(to_monster("nonexistent critter")["blue_vs_red_team"]);""").trim(),
        )
    }
}
