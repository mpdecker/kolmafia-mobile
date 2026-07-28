package net.sourceforge.kolmafia.modifiers

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase

class CurrentModifiersCodpieceTest {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun codpieceGem_muscleModifier_accumulatesWhenSlotsPopulated() {
        val gemName = "test muscle gem"
        ModifierDatabase.injectForTest("EternityCodpiece", gemName, "Muscle: +5")
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.CODPIECE1 to gemName),
        )
        val mods = CurrentModifiers(state)
        assertEquals(5, mods.values.getInt(DoubleModifier.MUS))
    }

    @Test
    fun codpieceGem_skippedWhenNoCodpieceContext() {
        ModifierDatabase.injectForTest("EternityCodpiece", "unused gem", "Muscle: +5")
        val mods = CurrentModifiers(CharacterState())
        assertEquals(0, mods.values.getInt(DoubleModifier.MUS))
    }
}
