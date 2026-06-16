package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP19Test {

    private suspend fun loadedLib(): GameRuntimeLibrary {
        val db = GameDatabase()
        db.load()
        return GameRuntimeLibrary(gameDatabase = db)
    }

    @Test
    fun numericModifier_location_brinyDeepsItemDropPenalty() = runBlocking {
        val lib = loadedLib()
        val out = outputLib(
            lib,
            """print(to_string(numeric_modifier(to_location("The Briny Deeps"), "Item Drop Penalty")));""",
        )
        assertEquals("-25.0", out)
    }

    @Test
    fun numericModifier_path_youRobotEnergy() = runBlocking {
        val lib = loadedLib()
        val out = outputLib(
            lib,
            """print(to_string(numeric_modifier(to_path("You, Robot"), "Energy")));""",
        )
        assertEquals("1.0", out)
    }

    @Test
    fun numericModifier_thrall_spiceGhostItemDrop() = runBlocking {
        val lib = loadedLib()
        val out = outputLib(
            lib,
            """print(to_string(numeric_modifier(to_thrall("Spice Ghost"), "Item Drop")));""",
        )
        assertEquals("10.0", out)
    }

    @Test
    fun booleanModifier_location_unknownReturnsFalse() = runBlocking {
        val lib = loadedLib()
        assertEquals(
            "false",
            outputLib(
                lib,
                """print(to_string(boolean_modifier(to_location("No Such Zone"), "Underwater Familiar")));""",
            ),
        )
    }

    @Test
    fun isValid_location_brinyDeeps() = runBlocking {
        val lib = loadedLib()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_location("The Briny Deeps"))));"""),
        )
    }

    @Test
    fun isValid_location_nonsense() = runBlocking {
        val lib = loadedLib()
        assertFalse(
            outputLib(lib, """print(to_string(is_valid(to_location("Not A Real Location 9999"))));""")
                .toBooleanStrictOrNull() ?: false,
        )
    }
}
