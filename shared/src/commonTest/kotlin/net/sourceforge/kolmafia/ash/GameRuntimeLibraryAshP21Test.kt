package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP21Test {

    @Test
    fun outfitModifier_returnsLiveMuscleValue() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "25.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier("Outfit:Antique Nutcracker Outfit", "Muscle")));""",
            ),
        )
    }

    @Test
    fun signModifier_returnsLiveColdResistance() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier("Sign:Marmot", "Cold Resistance")));""",
            ),
        )
    }
}
