package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.TCRSDatabase

class GameRuntimeLibraryAshP247Test {

    @BeforeTest
    fun setUp() = runTest {
        TCRSDatabase.reset()
        GameDatabase().load()
    }

    @AfterTest
    fun tearDown() {
        TCRSDatabase.reset()
    }

    @Test
    fun revision_isphase236() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun itemBracket_tcrsName_unmappedReturnsRegularName() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "hot wing",
            outputLib(lib, """print(to_item("hot wing")["tcrs_name"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_tcrsName_returnsRegisteredMapping() = runTest {
        TCRSDatabase.registerForTest(471, "bouncing spicy batwing")
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "bouncing spicy batwing",
            outputLib(lib, """print(to_item("hot wing")["tcrs_name"]);""").trim(),
        )
    }

    @Test
    fun itemBracket_tcrsName_loadFromPreferences() = runTest {
        val prefs = prefs()
        val fixture = buildString {
            appendLine("471\tbouncing spicy batwing\t1\t\tEffect: \"Spicy\", Effect Duration: 5")
            appendLine("1\tmirror seal-clubbing club\t0\t\t")
        }
        TCRSDatabase.load("Seal Clubber", "Mongoose", fixture)
        TCRSDatabase.saveToPreferences("Seal Clubber", "Mongoose", prefs)
        TCRSDatabase.reset()
        TCRSDatabase.loadFromPreferences("Seal Clubber", "Mongoose", prefs)
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "bouncing spicy batwing",
            outputLib(lib, """print(to_item("hot wing")["tcrs_name"]);""").trim(),
        )
    }
}
