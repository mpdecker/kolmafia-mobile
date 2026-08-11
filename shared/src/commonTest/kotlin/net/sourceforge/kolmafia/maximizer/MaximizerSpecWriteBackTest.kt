package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MaximizerSpecWriteBackTest {

    @BeforeTest
    fun setUp() {
        ModifierDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun writeFromPlan_storesGeneratedSpecWithBestLoadoutModifiers() {
        ModifierDatabase.injectForTest("Item", "meat ring", "Meat Drop: +25")
        val plan = MaximizerEmitSlot.Plan(
            goal = "meat",
            spec = MaximizeGoal.parseSpec("meat")!!,
            scoreBefore = 0.0,
            scoreAfter = 25.0,
            bestPerSlot = mapOf(EquipmentSlot.ACC1 to ("meat ring" to 25.0)),
        )
        MaximizerSpecWriteBack.writeFromPlan(
            plan = plan,
            charState = CharacterState(level = 15),
        )
        val entry = ModifierDatabase.get("Generated", "_spec")
        requireNotNull(entry)
        val values = ModifierParser.parse(entry.modifiers)
        assertEquals(25.0, values.get(DoubleModifier.MEATDROP))
    }

    @Test
    fun writeFromLiveState_storesGeneratedSpecFromEquippedState() {
        ModifierDatabase.injectForTest("Item", "plain hat", "Mysticality: +1")
        ModifierDatabase.injectForTest("Item", "myst hat", "Mysticality: +5")
        MaximizerSpecWriteBack.writeFromLiveState(
            charState = CharacterState(
                level = 15,
                equipment = mapOf(EquipmentSlot.HAT to "plain hat"),
            ),
        )
        val entry = ModifierDatabase.get("Generated", "_spec")
        requireNotNull(entry)
        val values = ModifierParser.parse(entry.modifiers)
        assertEquals(1.0, values.get(DoubleModifier.MYS))
    }
}
