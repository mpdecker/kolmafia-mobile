package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.utilities.leetify

class MonsterDatabaseLeetTest {

    @Test
    fun translateLeetMonsterName_resolvesNaughtySorceress() = runBlocking {
        MonsterDatabase.load()
        val leetName = leetify("Naughty Sorceress")
        assertEquals("Naughty Sorceress", MonsterDatabase.translateLeetMonsterName(leetName))
    }

    @Test
    fun translateLeetMonsterName_unknownReturnsInput() = runBlocking {
        MonsterDatabase.load()
        assertEquals("unknown 1337 name", MonsterDatabase.translateLeetMonsterName("unknown 1337 name"))
    }
}
