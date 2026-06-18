package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP24Test {

    @Test
    fun classStringModifier_returnsStatTuning() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "Muscle",
            outputLib(
                lib,
                """print(string_modifier(to_class("Seal Clubber"), "Stat Tuning"));""",
            ).trim(),
        )
        assertEquals(
            "Mysticality",
            outputLib(
                lib,
                """print(string_modifier(to_class("Pastamancer"), "Stat Tuning"));""",
            ).trim(),
        )
        assertEquals(
            "Moxie",
            outputLib(
                lib,
                """print(string_modifier(to_class("Disco Bandit"), "Stat Tuning"));""",
            ).trim(),
        )
    }

    @Test
    fun classNumericModifier_unknownTagReturnsZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_class("Seal Clubber"), "Muscle")));""",
            ).trim(),
        )
    }

    @Test
    fun towerDoorAsh_runsWithoutError() {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(to_string(tower_door()));""").trim())
    }
}
