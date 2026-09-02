package net.sourceforge.kolmafia.modifiers

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase

class CurrentModifiersOutfitBitmapTest {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun singleBrimstonePiece_appliesScaledSetBonus() {
        ModifierDatabase.injectForTest("Item", "Brimstone Beret", "Muscle: +1, Brimstone")
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.HAT to "Brimstone Beret"),
        )
        val mods = CurrentModifiers(state)
        assertEquals(2.0, mods.values.get(DoubleModifier.MONSTER_LEVEL))
    }

    @Test
    fun twoBrimstonePieces_appliesScaledSetBonus() {
        ModifierDatabase.injectForTest("Item", "Brimstone Beret", "Brimstone")
        ModifierDatabase.injectForTest("Item", "Brimstone Boxers", "Brimstone")
        val state = CharacterState(
            equipment = mapOf(
                EquipmentSlot.HAT to "Brimstone Beret",
                EquipmentSlot.PANTS to "Brimstone Boxers",
            ),
        )
        val mods = CurrentModifiers(state)
        assertEquals(4.0, mods.values.get(DoubleModifier.MONSTER_LEVEL))
        assertEquals(4.0, mods.values.get(DoubleModifier.MEATDROP))
        assertEquals(4.0, mods.values.get(DoubleModifier.ITEMDROP))
    }

    @Test
    fun twoCloathingPieces_appliesScaledSetBonus() {
        ModifierDatabase.injectForTest("Item", "Hodgman simulacrum", "Cloathing")
        ModifierDatabase.injectForTest("Item", "Hodgman call", "Cloathing")
        val state = CharacterState(
            equipment = mapOf(
                EquipmentSlot.HAT to "Hodgman simulacrum",
                EquipmentSlot.WEAPON to "Hodgman call",
            ),
        )
        val mods = CurrentModifiers(state)
        assertEquals(4.0, mods.values.get(DoubleModifier.MUS_PCT))
        assertEquals(4.0, mods.values.get(DoubleModifier.MYS_PCT))
        assertEquals(4.0, mods.values.get(DoubleModifier.MOX_PCT))
        assertEquals(4.0, mods.values.get(DoubleModifier.MEATDROP))
        assertEquals(2.0, mods.values.get(DoubleModifier.ITEMDROP))
    }

    @Test
    fun twoMcHugeLargePieces_appliesSetBonus() {
        ModifierDatabase.injectForTest("Item", "McHugeLarge left pole", "McHugeLarge")
        ModifierDatabase.injectForTest("Item", "McHugeLarge right pole", "McHugeLarge")
        val state = CharacterState(
            equipment = mapOf(
                EquipmentSlot.OFFHAND to "McHugeLarge left pole",
                EquipmentSlot.WEAPON to "McHugeLarge right pole",
            ),
        )
        val mods = CurrentModifiers(state)
        assertEquals(2.0, mods.values.get(DoubleModifier.COLD_RESISTANCE))
        assertEquals(10.0, mods.values.get(DoubleModifier.HOT_DAMAGE))
        assertEquals(20.0, mods.values.get(DoubleModifier.INITIATIVE))
    }

    @Test
    fun mcHugeLargePieces_suppressedInNoobcore() {
        ModifierDatabase.injectForTest("Item", "McHugeLarge left pole", "McHugeLarge")
        ModifierDatabase.injectForTest("Item", "McHugeLarge right pole", "McHugeLarge")
        val state = CharacterState(
            challengePath = "Gelatinous Noob",
            equipment = mapOf(
                EquipmentSlot.OFFHAND to "McHugeLarge left pole",
                EquipmentSlot.WEAPON to "McHugeLarge right pole",
            ),
        )
        val mods = CurrentModifiers(state)
        assertEquals(0.0, mods.values.get(DoubleModifier.COLD_RESISTANCE))
    }
}
