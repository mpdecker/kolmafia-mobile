package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class MonsterDatabaseDefenseElementTest {

    @Test
    fun parse_ed_only_setsDefenseElement() = runBlocking {
        MonsterDatabase.load()
        // caveman frat boy has ED: sleaze and no EA
        val monster = MonsterDatabase.getByName("caveman frat boy")!!
        assertEquals("sleaze", monster.defenseElement)
        assertEquals("", monster.attackElement)
    }

    @Test
    fun parse_ed_and_ea_canDiffer() = runBlocking {
        MonsterDatabase.load()
        // Axe Wound: ED: sleaze EA: cold
        val monster = MonsterDatabase.getByName("Axe Wound")!!
        assertEquals("sleaze", monster.defenseElement)
        assertEquals("cold", monster.attackElement)
    }

    @Test
    fun parse_noEd_leavesDefenseEmpty() = runBlocking {
        MonsterDatabase.load()
        assertEquals("", MonsterDatabase.getByName("huge mosquito")?.defenseElement)
    }
}
