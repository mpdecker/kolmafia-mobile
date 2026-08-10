package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.RestoreDatabase

class GameRuntimeLibraryAshP242Test {

    @BeforeTest
    fun setUp() = runTest {
        GameDatabase().load()
    }

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
        RestoreDatabase.resetForTest()
    }

    @Test
    fun revision_isphase226() {
        assertEquals("phase380", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun itemBracket_minhpSmoke() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "101",
            outputLib(lib, """print(to_item("aspirin")["minhp"]);""").trim(),
        )
        assertEquals(
            "30",
            outputLib(lib, """print(to_item("ancient pills")["minmp"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_batteryMpRespectsPath() = runTest {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = AscensionPath.YOU_ROBOT.apiName))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "0",
            outputLib(lib, """print(to_item("battery (AAA)")["maxmp"]);""").trim(),
        )
    }
}
