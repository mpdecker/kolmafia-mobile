package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class GoalManagerPhase4251Test {

    @Test
    fun goalCountReturnsRemainingChoiceAdventures() {
        val manager = GoalManager()
        manager.setChoiceAdventureGoal(3)
        assertEquals(3, manager.goalCount("choice"))
        manager.noteChoiceAdventureCompleted()
        assertEquals(2, manager.goalCount("choices"))
    }

    @Test
    fun goalCountReturnsRemainingPseudoProgress() {
        val manager = GoalManager()
        val prefs = Preferences(MapSettings())
        manager.setPseudoGoal(GoalPseudoConditions.Kind.PIRATE_INSULT, 5)
        prefs.setBoolean("lastPirateInsult1", true)
        prefs.setBoolean("lastPirateInsult2", true)
        assertEquals(3, manager.goalCount("pirate insult", prefs))
    }

    @Test
    fun goalCountReturnsRemainingHealth() {
        val manager = GoalManager()
        val state = CharacterState(currentHp = 40, currentMp = 10, maxHp = 100, maxMp = 50)
        manager.setResourceGoal(GoalManager.ResourceKind.HEALTH, 80)
        assertEquals(40, manager.goalCount("health", state = state))
    }

    @Test
    fun goalCountReturnsRemainingMeat() {
        val manager = GoalManager()
        manager.setMeatGoal(10_000)
        val state = CharacterState(meat = 7_500)
        assertEquals(2_500, manager.goalCount("meat", state = state))
        assertEquals(0, manager.goalCount("meat", state = state.copy(meat = 12_000)))
    }

    @Test
    fun goalCountReturnsRemainingLevel() {
        val manager = GoalManager()
        manager.setLevelGoal(15)
        val state = CharacterState(level = 12)
        assertEquals(3, manager.goalCount("level", state = state))
        assertEquals(0, manager.goalCount("level", state = state.copy(level = 20)))
    }

    @Test
    fun goalCountReturnsRemainingItemGoals() {
        val manager = GoalManager()
        manager.addItemGoal(100, 5)
        manager.addItemGoal(200, 3)
        assertEquals(8, manager.goalCount("item"))
        assertEquals(6, manager.goalCount("items", inventoryCount = { id ->
            when (id) {
                100 -> 2
                else -> 0
            }
        }))
    }
}
