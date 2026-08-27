package net.sourceforge.kolmafia.session

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.MonsterDefinition

class FightMonsterHealthSyncTest {

    @BeforeTest
    fun setUp() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "test monster",
                id = 1,
                image = "test.gif",
                attack = 100,
                defense = 80,
                hp = 200,
                initiative = 50,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
            ),
            emptyList(),
        )
    }

    @Test
    fun detectiveSkullSetsHealth() {
        assertTrue(
            FightMonsterHealthSync.apply(
                "I deduce that this monster has approximately 150 hit points left.",
            ),
        )
        assertEquals(150, MonsterStatusTracker.getMonsterHealth())
    }

    @Test
    fun spaceHelmetSetsHealth() {
        FightMonsterHealthSync.apply("Opponent HP: 42")
        assertEquals(42, MonsterStatusTracker.getMonsterHealth())
    }

    @Test
    fun manuelStatsApplyModifiers() {
        val html = """
            <img alt="Enemy's Attack Power"><td>90</td>
            <img alt="Enemy's Defense"><td>70</td>
            <img alt="Enemy's Hit Points"><td>120</td>
        """.trimIndent()
        assertTrue(FightMonsterHealthSync.applyManuel(html))
        assertEquals(90, MonsterStatusTracker.getMonsterAttack())
        assertEquals(70, MonsterStatusTracker.getMonsterDefense())
        assertEquals(120, MonsterStatusTracker.getMonsterHealth())
    }

    @Test
    fun sorceressResetsAtkDef() {
        MonsterStatusTracker.lowerMonsterAttack(10)
        MonsterStatusTracker.lowerMonsterDefense(5)
        FightMonsterHealthSync.apply(
            "The Sorceress pauses for a moment, mutters some words under her breath, and straightens out her dress. Her skin seems to shimmer for a moment.",
        )
        assertEquals(0, MonsterStatusTracker.getMonsterAttackModifier())
        assertEquals(0, MonsterStatusTracker.getMonsterDefenseModifier())
    }
}
