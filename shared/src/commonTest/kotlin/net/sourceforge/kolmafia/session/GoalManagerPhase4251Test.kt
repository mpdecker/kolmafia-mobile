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
}
