package net.sourceforge.kolmafia.modifiers

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase

class CurrentModifiersThroneBjornTest {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun crownWorn_enthronedRace_appliesThroneModifiers() {
        ModifierDatabase.injectForTest("Throne", "Angry Goat", "Muscle Percent: +15")
        val state = CharacterState(
            baseMusc = 100,
            equipment = mapOf(EquipmentSlot.HAT to "Crown of Thrones"),
            enthronedFamiliarName = "Angry Goat",
        )
        val mods = CurrentModifiers(state)
        assertEquals(15.0, mods.values.get(DoubleModifier.MUS_PCT))
    }

    @Test
    fun bjornWorn_bjornedRace_appliesThroneModifiersViaBjornAlias() {
        ModifierDatabase.injectForTest("Throne", "Seal Larva", "Muscle: +5")
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.CONTAINER to "Buddy Bjorn"),
            bjornedFamiliarName = "Seal Larva",
        )
        val mods = CurrentModifiers(state)
        assertEquals(5, mods.values.getInt(DoubleModifier.MUS))
    }

    @Test
    fun crownWorn_noEnthronedRace_skipsThroneModifiers() {
        ModifierDatabase.injectForTest("Throne", "Angry Goat", "Muscle Percent: +15")
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.HAT to "Crown of Thrones"),
            enthronedFamiliarName = "",
        )
        val mods = CurrentModifiers(state)
        assertEquals(0.0, mods.values.get(DoubleModifier.MUS_PCT))
    }

    @Test
    fun enthronedRaceSet_crownNotWorn_skipsThroneModifiers() {
        ModifierDatabase.injectForTest("Throne", "Angry Goat", "Muscle Percent: +15")
        val state = CharacterState(
            equipment = emptyMap(),
            enthronedFamiliarName = "Angry Goat",
        )
        val mods = CurrentModifiers(state)
        assertEquals(0.0, mods.values.get(DoubleModifier.MUS_PCT))
    }

    @Test
    fun throneNoneModifier_skipped() {
        ModifierDatabase.injectForTest("Throne", "Arachnelf", "none")
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.HAT to "Crown of Thrones"),
            enthronedFamiliarName = "Arachnelf",
        )
        val mods = CurrentModifiers(state)
        assertEquals(0.0, mods.values.get(DoubleModifier.MUS_PCT))
    }
}
