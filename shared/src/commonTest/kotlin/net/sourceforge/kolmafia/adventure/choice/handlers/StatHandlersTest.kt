package net.sourceforge.kolmafia.adventure.choice.handlers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
import net.sourceforge.kolmafia.adventure.choice.ChoiceSolvers
import net.sourceforge.kolmafia.adventure.choice.EffectPool
import net.sourceforge.kolmafia.adventure.choice.solvers.ArcadeGameSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.GameproSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LightsOutSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LostKeySolver
import net.sourceforge.kolmafia.adventure.choice.solvers.SafetyShelterSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.VampOutSolver
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatHandlersTest {

    private val prefs = Preferences(MapSettings())
    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    private fun ctx(
        choiceId: Int,
        preference: Int = 0,
        charState: CharacterState = CharacterState(),
        effects: List<EffectData> = emptyList(),
        options: Map<Int, String> = (1..6).associateWith { "O$it" },
    ) = ChoiceContext(
        choiceId = choiceId, options = options,
        responseText = "", characterState = charState,
        inventoryState = InventoryState(),
        effectState = EffectState(effects = effects),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = noOpSolvers, preference = preference,
    )

    private fun effect(name: String) = EffectData(id = 0, name = name, duration = 10)

    // Case 89
    @Test fun case89_pref1_returns1() = assertEquals(1, StatHandlers.handlers[89]?.decide(ctx(89, 1)))
    @Test fun case89_pref2_returns2() = assertEquals(2, StatHandlers.handlers[89]?.decide(ctx(89, 2)))
    @Test fun case89_pref6_returns4() = assertEquals(4, StatHandlers.handlers[89]?.decide(ctx(89, 6)))
    @Test fun case89_pref4_hasMaiden_returns1() {
        val result = StatHandlers.handlers[89]?.decide(ctx(89, 4, effects = listOf(effect(EffectPool.MAIDEN_EFFECT))))
        assertEquals(1, result)
    }
    @Test fun case89_pref4_noMaiden_returns3() {
        val result = StatHandlers.handlers[89]?.decide(ctx(89, 4))
        assertEquals(3, result)
    }
    @Test fun case89_pref5_hasMaiden_returns2() {
        val result = StatHandlers.handlers[89]?.decide(ctx(89, 5, effects = listOf(effect(EffectPool.MAIDEN_EFFECT))))
        assertEquals(2, result)
    }
    @Test fun case89_pref3_noMaiden_returns3() = assertEquals(3, StatHandlers.handlers[89]?.decide(ctx(89, 3)))
    @Test fun case89_pref0_returnsInRange() {
        val result = StatHandlers.handlers[89]?.decide(ctx(89, 0))
        assertNotNull(result)
        assertTrue(result in 1..2)
    }

    // Case 162
    @Test fun case162_pref2_returns2() =
        assertEquals(2, StatHandlers.handlers[162]?.decide(ctx(162, 2)))
    @Test fun case162_axecore_returns3() {
        val char = CharacterState(challengePath = "Avatar of Boris")
        assertEquals(3, StatHandlers.handlers[162]?.decide(ctx(162, charState = char)))
    }
    @Test fun case162_fistcore_earthenFist_returns1() {
        val char = CharacterState(challengePath = "Way of the Surprising Fist", kingLiberated = false)
        val result = StatHandlers.handlers[162]?.decide(
            ctx(162, charState = char, effects = listOf(effect(EffectPool.EARTHEN_FIST)))
        )
        assertEquals(1, result)
    }
    @Test fun case162_default_returns2() =
        assertEquals(2, StatHandlers.handlers[162]?.decide(ctx(162)))

    // Case 184
    @Test fun case184_muscle_pref4_returns3() {
        val char = CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id)
        assertEquals(3, StatHandlers.handlers[184]?.decide(ctx(184, 4, char)))
    }
    @Test fun case184_muscle_pref5_returns2() {
        val char = CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id)
        assertEquals(2, StatHandlers.handlers[184]?.decide(ctx(184, 5, char)))
    }
    @Test fun case184_myst_pref4_returns1() {
        val char = CharacterState(characterClass = CharacterClass.PASTAMANCER.id)
        assertEquals(1, StatHandlers.handlers[184]?.decide(ctx(184, 4, char)))
    }
    @Test fun case184_moxie_pref4_returns2() {
        val char = CharacterState(characterClass = CharacterClass.DISCO_BANDIT.id)
        assertEquals(2, StatHandlers.handlers[184]?.decide(ctx(184, 4, char)))
    }
    @Test fun case184_other_pref2_returns2() =
        assertEquals(2, StatHandlers.handlers[184]?.decide(ctx(184, 2)))

    // Case 700
    @Test fun case700_pref1_hasJock_returns1() {
        val result = StatHandlers.handlers[700]?.decide(ctx(700, 1, effects = listOf(effect(EffectPool.JOCK_EFFECT))))
        assertEquals(1, result)
    }
    @Test fun case700_pref1_hasNerd_returns2() {
        val result = StatHandlers.handlers[700]?.decide(ctx(700, 1, effects = listOf(effect(EffectPool.NERD_EFFECT))))
        assertEquals(2, result)
    }
    @Test fun case700_pref1_noEffect_returns3() =
        assertEquals(3, StatHandlers.handlers[700]?.decide(ctx(700, 1)))
    @Test fun case700_pref2_returnsPreference() =
        assertEquals(2, StatHandlers.handlers[700]?.decide(ctx(700, 2)))

    // Case 1049
    @Test fun case1049_singleOption_returns1() {
        val result = StatHandlers.handlers[1049]?.decide(ctx(1049, options = mapOf(1 to "Boredom.")))
        assertEquals(1, result)
    }
    @Test fun case1049_sealClubber_picksBoredom() {
        val char = CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id)
        val opts = mapOf(1 to "Boredom.", 2 to "Friendship.", 3 to "Power.")
        assertEquals(1, StatHandlers.handlers[1049]?.decide(ctx(1049, charState = char, options = opts)))
    }
    @Test fun case1049_unknownClass_returnsNull() =
        assertNull(StatHandlers.handlers[1049]?.decide(ctx(1049)))

    // Case 1087
    @Test fun case1087_singleOption_returns1() {
        val result = StatHandlers.handlers[1087]?.decide(ctx(1087, options = mapOf(1 to "anything")))
        assertEquals(1, result)
    }
    @Test fun case1087_pastamancer_picksNoodles() {
        val char = CharacterState(characterClass = CharacterClass.PASTAMANCER.id)
        val opts = mapOf(1 to "Entangle the wall with noodles.", 2 to "Shoot a stream of sauce at the wall.")
        assertEquals(1, StatHandlers.handlers[1087]?.decide(ctx(1087, charState = char, options = opts)))
    }
}
