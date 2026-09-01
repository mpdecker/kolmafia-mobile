package net.sourceforge.kolmafia.modifiers

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.outputLib
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.maximizer.Evaluator
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombinedModifierLookupTest {

    @BeforeTest
    fun setUp() {
        ModifierDatabase.resetForTest()
        runBlocking { ModifierDatabase.load() }
    }

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    private val lib = GameRuntimeLibrary.forTesting()

    @Test
    fun numericModifier_combinedTag_resolvesMaximumHpMp() {
        assertEquals(
            "25.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_item("Apriling band helmet"), "Maximum HP / MP")));""",
            ),
        )
    }

    @Test
    fun numericModifier_subsumedMember_resolvesMaximumHp() {
        assertEquals(
            "25.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_item("Apriling band helmet"), "Maximum HP")));""",
            ),
        )
    }

    @Test
    fun numericModifier_combinedPercentTag_resolvesAllAttributesPercent() {
        assertEquals(
            "10.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_item("Apriling band helmet"), "All Attributes Percent")));""",
            ),
        )
    }

    @Test
    fun numericModifier_subsumedPercentMember_resolvesMusclePercent() {
        assertEquals(
            "10.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_item("Apriling band helmet"), "Muscle Percent")));""",
            ),
        )
    }

    @Test
    fun currentModifiers_buffedHp_includesMaximumHpMpFromCombinedTag() {
        val bare = CurrentModifiers(CharacterState(baseMaxHp = 30))
        val equipped = CurrentModifiers(
            CharacterState(
                baseMaxHp = 30,
                equipment = mapOf(EquipmentSlot.HAT to "Apriling band helmet"),
            ),
        )
        assertEquals(25.0, equipped.values.get(DoubleModifier.MAXIMUM_HP_MP))
        assertEquals(25.0, equipped.values.get(DoubleModifier.HP))
        assertTrue(equipped.buffedHp() > bare.buffedHp())
        assertEquals(bare.buffedHp() + 25, equipped.buffedHp())
    }

    @Test
    fun evaluator_hpGoal_scoresSubsumedHpWithoutDoubleCountingCombinedTag() {
        val parsed = ModifierParser.parse(
            ModifierDatabase.getItem("Apriling band helmet")!!.modifiers,
        )
        val eval = Evaluator("hp")
        val contribution = eval.getItemContribution(parsed)
        assertEquals(25.0, contribution)
    }

    @Test
    fun evaluator_muscleGoal_scoresSubsumedMuscleWithoutDoubleCounting() {
        val parsed = ModifierParser.parse(
            ModifierDatabase.getItem("anniversary concrete fedora")!!.modifiers,
        )
        val eval = Evaluator("muscle")
        val contribution = eval.getItemContribution(parsed)
        assertEquals(1.0, contribution)
    }
}
