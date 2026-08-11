package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.PokefamTeamSlot
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP418Test {

    @Test
    fun my_poke_fam_readsTeamSlots() = runBlocking {
        val db = GameDatabase()
        db.load()
        val character = KoLCharacter()
        character.updatePokeTeam(
            listOf(
                PokefamTeamSlot(familiarId = 215, name = "Globmule", level = 12),
                PokefamTeamSlot(familiarId = 216, name = "Bluzzard", level = 8),
                PokefamTeamSlot(familiarId = 217, name = "Faux", level = 5),
            ),
        )
        val lib = GameRuntimeLibrary(gameDatabase = db, character = character)
        assertEquals("Globmule", outputLib(lib, """print(my_poke_fam(0));""").trim())
        assertEquals("Bluzzard", outputLib(lib, """print(my_poke_fam(1));""").trim())
        assertEquals("Faux", outputLib(lib, """print(my_poke_fam(2));""").trim())
    }

    @Test
    fun my_poke_fam_outOfRangeAndEmptyReturnNone() = runBlocking {
        val db = GameDatabase()
        db.load()
        val character = KoLCharacter()
        character.updatePokeTeam(
            listOf(
                PokefamTeamSlot(familiarId = 215, name = "Globmule", level = 4),
                PokefamTeamSlot.EMPTY,
                PokefamTeamSlot.EMPTY,
            ),
        )
        val lib = GameRuntimeLibrary(gameDatabase = db, character = character)
        assertEquals("none", outputLib(lib, """print(my_poke_fam(-1));""").trim())
        assertEquals("none", outputLib(lib, """print(my_poke_fam(3));""").trim())
        assertEquals("none", outputLib(lib, """print(my_poke_fam(1));""").trim())
    }

    @Test
    fun revision_phase418() {
        assertEquals("phase450", GameRuntimeLibrary.REVISION)
    }
}
