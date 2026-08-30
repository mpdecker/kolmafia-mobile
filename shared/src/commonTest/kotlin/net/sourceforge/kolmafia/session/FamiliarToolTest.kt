package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.FamiliarDefinition
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.session.CakeArenaManager.ArenaOpponent
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FamiliarToolTest {

    @BeforeTest
    fun seed() {
        FamiliarDefinitionDatabase.registerForTest(
            FamiliarDefinition(
                id = 1,
                name = "Mosquito",
                image = "familiar1.gif",
                types = setOf("combat0"),
                larvaItem = "mosquito larva",
                hatchlingItem = "mosquito larva",
                arenaCombatMoves = 2,
                arenaStrength = 1,
                arenaOc = 1,
                arenaHs = 2,
                attributes = emptySet(),
            ),
        )
        FamiliarDefinitionDatabase.registerForTest(
            FamiliarDefinition(
                id = 2,
                name = "Baby Gravy Fairy",
                image = "familiar2.gif",
                types = setOf("item0"),
                larvaItem = "",
                hatchlingItem = "",
                arenaCombatMoves = 1,
                arenaStrength = 2,
                arenaOc = 3,
                arenaHs = 1,
                attributes = emptySet(),
            ),
        )
    }

    @Test
    fun bestOpponent_picksMatch() {
        val opponents = listOf(
            ArenaOpponent(1, "Pork Soda", "Baby Gravy Fairy", 15),
            ArenaOpponent(2, "Citrus", "Baby Gravy Fairy", 25),
        )
        val tool = FamiliarTool(opponents)
        val best = tool.bestOpponent(1, intArrayOf(10, 15, 20))
        assertNotNull(best)
        assertTrue(tool.bestMatch() in 1..4)
        assertTrue(tool.bestWeight() in intArrayOf(10, 15, 20).toList())
    }

    @Test
    fun goalEnum_values() {
        assertEquals(4, FamiliarTrainingManager.Goal.entries.size)
        assertTrue(FamiliarDefinitionDatabase.isTrainable(1))
        assertEquals(intArrayOf(2, 1, 1, 2).toList(), FamiliarDefinitionDatabase.getFamiliarSkills(1).toList())
    }
}
