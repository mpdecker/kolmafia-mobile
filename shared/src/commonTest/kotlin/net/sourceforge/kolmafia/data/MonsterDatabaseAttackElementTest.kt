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
    fun parse_ea_noneAlone_leavesEmpty() = runBlocking {
        MonsterDatabase.load()
        // Anime Smiley only has EA: "bad spelling" (quoted factoid) — no elemental EA
        assertEquals("", MonsterDatabase.getByName("Anime Smiley")?.attackElement)
    }

    @Test
    fun parse_ea_skipsNoneBeforeElement() = runBlocking {
        MonsterDatabase.load()
        // A.M.C. gremlin: EA: hot EA: none → first elemental is hot
        assertEquals("hot", MonsterDatabase.getByName("Astronomer")?.attackElement)
    }
}
