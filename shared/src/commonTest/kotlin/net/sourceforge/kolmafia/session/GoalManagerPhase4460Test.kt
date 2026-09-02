package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.OutfitData
import net.sourceforge.kolmafia.data.OutfitDatabase

class GoalManagerPhase4460Test {

    @Test
    fun matchesConditionTypeHandlesDesktopAliases() {
        val manager = GoalManager()
        manager.setFloundryGoal(2)
        assertTrue(manager.matchesConditionType("floundry fish"))
        manager.setLeprecondoGoal(1)
        assertTrue(manager.matchesConditionType("leprecondo furniture"))
        manager.setChoiceAdventureGoal(3)
        assertTrue(manager.matchesConditionType("choiceadv"))
    }

    @Test
    fun goalCountReturnsOneForTextFactoidGoal() {
        val manager = GoalManager()
        manager.setFactoidGoal("learned a new fact")
        assertEquals(1, manager.goalCount("factoid"))
    }

    @Test
    fun outfitGoalActiveSeparatesOutfitFromItemGoals() {
        OutfitDatabase.registerStatic(
            OutfitData(
                id = 5,
                name = "Knob Goblin Elite Guard Uniform",
                image = "eliteguard.gif",
                equipment = listOf("Knob Goblin elite helm", "Knob Goblin elite polearm", "Knob Goblin elite pants"),
                halloweenDrops = emptyList(),
            ),
        )
        val manager = GoalManager()
        GoalOutfitConditions.addOutfitConditions(
            location = "cobb's knob barracks",
            manager = manager,
            mode = GoalManager.ConditionMode.ADD,
            isEquipped = { false },
        )
        assertTrue(manager.outfitGoalActive)
        assertTrue(manager.matchesConditionType("outfit"))
        assertEquals(false, manager.matchesConditionType("item"))
    }
}
