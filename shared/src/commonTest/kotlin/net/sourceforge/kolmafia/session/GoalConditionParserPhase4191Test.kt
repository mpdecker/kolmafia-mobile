package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences

class GoalConditionParserPhase4191Test {

    @Test
    fun parsePirateInsultCondition() {
        val parsed = GoalConditionParser.parse("5 pirate insults")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.PIRATE_INSULT, parsed.kind)
        assertEquals(5, parsed.count)
    }

    @Test
    fun parseArenaFlyerMl() {
        val parsed = GoalConditionParser.parse("3 arena flyer ml")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.ARENA_FLYER_ML, parsed.kind)
    }

    @Test
    fun parseChasmBridge() {
        val parsed = GoalConditionParser.parse("chasm bridge")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.CHASM_BRIDGE, parsed.kind)
        assertEquals(GoalPseudoConditions.MAX_CHASM_PROGRESS, parsed.count)
    }

    @Test
    fun parseSubstatPoints() {
        val parsed = GoalConditionParser.parse("100 muscle")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.SUBSTAT_POINTS, parsed.kind)
        assertEquals(0, parsed.statIndex)
    }

    @Test
    fun parseHealthPercent() {
        val parsed = GoalConditionParser.parse("50% health")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.HEALTH, parsed.kind)
        assertTrue(parsed.percent)
    }

    @Test
    fun parseOutfitLocation() {
        val parsed = GoalConditionParser.parse("treasury outfit")
        assertNotNull(parsed)
        assertEquals(GoalConditionParser.ParsedCondition.Kind.OUTFIT, parsed.kind)
        assertEquals("treasury", parsed.outfitLocation)
    }
}

class GoalManagerPhase4191Test {

    @Test
    fun pseudoGoalMetWhenPrefsReachTarget() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("lastPirateInsult1", true)
        prefs.setBoolean("lastPirateInsult2", true)
        prefs.setBoolean("lastPirateInsult3", true)
        val manager = GoalManager()
        manager.setPseudoGoal(GoalPseudoConditions.Kind.PIRATE_INSULT, 3)
        assertTrue(manager.hasPseudoGoalMet(prefs))
    }

    @Test
    fun levelSubstatsUsesPrimeStat() {
        val manager = GoalManager()
        val state = CharacterState(characterClass = 1, baseMusc = 10, muscSubpoints = 0)
        manager.applyCondition(
            GoalConditionParser.parse("level 12")!!,
            GoalManager.ConditionMode.SET,
            GoalManager.ConditionContext(characterState = state),
        )
        assertTrue(manager.hasSubstatsGoal())
    }

    @Test
    fun healthGoalUsesCurrentHpTarget() {
        val manager = GoalManager()
        val state = CharacterState(currentHp = 50, maxHp = 200)
        manager.applyCondition(
            GoalConditionParser.parse("100 health")!!,
            GoalManager.ConditionMode.SET,
            GoalManager.ConditionContext(characterState = state),
        )
        assertTrue(manager.hasHealthGoal())
        assertTrue(manager.hasResourceGoalMet(state.copy(currentHp = 100)))
    }
}
