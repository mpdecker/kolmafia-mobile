package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP245Test {

    @BeforeTest
    fun setUp() = runTest {
        GameDatabase().load()
    }

    @Test
    fun revision_isphase236() {
        assertEquals("phase410", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun itemBracket_dailyUsesLeftSmoke() = runTest {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    fullness = "10",
                    stomachsize = "15",
                ),
            )
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            character = char,
            preferences = Preferences(MapSettings()),
        )
        assertEquals(
            "5",
            outputLib(lib, """print(to_item("hot wing")["dailyusesleft"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_dailyUseLimitExhausted() = runTest {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_bagOfCandyUsed", true)
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            preferences = prefs,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_item("chester's bag of candy")["dailyusesleft"]);""").trim(),
        )
    }
}
