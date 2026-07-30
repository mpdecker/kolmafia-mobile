package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class PokefamDatabaseTest {

    @Test
    fun load_angryGoatPokefamStats() = runBlocking {
        PokefamDatabase.load()
        val angryGoat = PokefamDatabase.getById(4)
        assertEquals(1, angryGoat?.power2)
        assertEquals(3, angryGoat?.hp2)
        assertEquals("Bite", angryGoat?.move1)
        assertEquals("Violent Shred", angryGoat?.move3)
        assertEquals("None", angryGoat?.attribute)
    }
}
