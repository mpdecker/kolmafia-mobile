package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class MonsterDatabaseAttackElementTest {

    @Test
    fun parse_ea_keepsFirstElementalAttack() = runBlocking {
        MonsterDatabase.load()
        assertEquals("spooky", MonsterDatabase.getByName("ancient protector spirit")?.attackElement)
        assertEquals("hot", MonsterDatabase.getByName("A.M.C. gremlin")?.attackElement)
    }

    @Test
    fun parse_ea_quotedBadSpelling() = runBlocking {
        MonsterDatabase.load()
        val smiley = MonsterDatabase.getByName("Anime Smiley")!!
        assertEquals(listOf("bad spelling"), canonicalElementOrder(smiley.attackElements))
        assertEquals("bad spelling", smiley.attackElement)
    }

    @Test
    fun parse_ea_enumOrderLastForMultiEa() = runBlocking {
        MonsterDatabase.load()
        // Astronomer: EA: hot EA: none → enum-order last is hot
        assertEquals("hot", MonsterDatabase.getByName("Astronomer")?.attackElement)
    }
}
