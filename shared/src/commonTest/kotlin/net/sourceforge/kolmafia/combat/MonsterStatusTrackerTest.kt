package net.sourceforge.kolmafia.combat

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.data.MonsterDatabase

class MonsterStatusTrackerTest {

    @BeforeTest
    fun resetTracker() {
        MonsterStatusTracker.resetLastMonster()
    }

    @Test
    fun setNextMonster_storesCopyWithModifiers() = runBlocking {
        MonsterDatabase.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        MonsterStatusTracker.setNextMonster(template, listOf("huge", "red-hot"))
        val instance = MonsterStatusTracker.getLastMonster()!!
        assertEquals("huge mosquito", instance.name)
        assertEquals(listOf("huge", "red-hot"), instance.randomModifiers)
        assertEquals("huge mosquito", MonsterStatusTracker.getLastMonsterName())
    }

    @Test
    fun resetLastMonster_clearsInstance() = runBlocking {
        MonsterDatabase.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        MonsterStatusTracker.setNextMonster(template, listOf("huge"))
        MonsterStatusTracker.resetLastMonster()
        assertNull(MonsterStatusTracker.getLastMonster())
        assertEquals("", MonsterStatusTracker.getLastMonsterName())
    }
}
