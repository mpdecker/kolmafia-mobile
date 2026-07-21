package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class MonsterBeeCountTest {

    @Test
    fun computeBeeCount_countsBInName() {
        assertEquals(3, MonsterDatabase.computeBeeCount("beefy bodyguard bat", 571))
    }

    @Test
    fun computeBeeCount_wanderingBeesExcluded() {
        assertEquals(0, MonsterDatabase.computeBeeCount("mumblebee", 1075))
    }

    @Test
    fun beeCount_fromParsedMonsters() = runBlocking {
        MonsterDatabase.load()
        val bat = MonsterDatabase.getByName("beefy bodyguard bat")!!
        assertEquals(3, bat.beeCount)
        val bee = MonsterDatabase.getByName("mumblebee")!!
        assertEquals(1075, bee.id)
        assertEquals(0, bee.beeCount)
    }
}
