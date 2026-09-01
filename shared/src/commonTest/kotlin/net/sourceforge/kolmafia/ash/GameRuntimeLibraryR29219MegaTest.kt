package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameRuntimeLibraryR29219MegaTest {

    @BeforeTest
    fun setUp() {
        ModifierDatabase.resetForTest()
        runBlocking { ModifierDatabase.load() }
    }

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun revision_isPhase4370() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun lastMaximizerSucceeded_isRegistered() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, "print(to_string(last_maximizer_succeeded()));"))
    }

    @Test
    fun combinedTags_parseFromModifiersFile() {
        val raw = ModifierDatabase.getItem("Apriling band helmet")?.modifiers
        assertNotNull(raw)
        val values = ModifierParser.parse(raw)
        assertEquals(25.0, values.get(DoubleModifier.MAXIMUM_HP_MP))
        assertEquals(10.0, values.get(DoubleModifier.ALL_ATTRIBUTES_PCT))
        assertEquals(25.0, values.get(DoubleModifier.HP))
        assertEquals(10.0, values.get(DoubleModifier.MUS_PCT))
    }

    @Test
    fun haxxorBlueVsRedTeam_isRed() = runBlocking {
        MonsterDatabase.load()
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "red",
            outputLib(lib, """print(to_monster("1335 HaXx0r")["blue_vs_red_team"]);""").trim(),
        )
    }
}
