package net.sourceforge.kolmafia.combat

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.MonsterDatabase

class RandomModifierStatsTest {

    @Test
    fun fragile_setsHpToOne() = runBlocking {
        MonsterDatabase.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        val modified = RandomModifierStats.apply(template, listOf("fragile"), null)
        assertEquals(1, modified.hp)
        assertEquals(true, modified.hasHp)
    }

    @Test
    fun tiny_dividesStatsByTen() = runBlocking {
        MonsterDatabase.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        val modified = RandomModifierStats.apply(template, listOf("tiny"), null)
        assertEquals(1, modified.hp)
        assertEquals(1, modified.attack)
        assertEquals(1, modified.defense)
    }

    @Test
    fun ghostly_setsPhysicalResistanceWhenZero() = runBlocking {
        MonsterDatabase.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        val modified = RandomModifierStats.apply(template, listOf("ghostly"), null)
        assertEquals(90, modified.physicalResistance)
    }

    @Test
    fun leftHanded_swapsAttackAndDefense() = runBlocking {
        MonsterDatabase.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        val modified = RandomModifierStats.apply(template, listOf("left-handed"), null)
        assertEquals(14, modified.attack)
        assertEquals(16, modified.defense)
    }

    @Test
    fun huge_doublesRawStats() = runBlocking {
        MonsterDatabase.load()
        val template = MonsterDatabase.getByName("huge mosquito")!!
        val modified = RandomModifierStats.apply(template, listOf("huge"), null)
        assertEquals(36, modified.hp)
        assertEquals(32, modified.attack)
        assertEquals(28, modified.defense)
    }
}
