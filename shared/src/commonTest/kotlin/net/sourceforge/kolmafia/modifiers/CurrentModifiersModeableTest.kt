package net.sourceforge.kolmafia.modifiers

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.equipment.Modeable

class CurrentModifiersModeableTest {

    @BeforeTest
    fun loadModifiers() {
        runBlocking { ModifierDatabase.load() }
    }

    @Test
    fun umbrella_bucketStyle_givesItemDrop() {
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.OFFHAND to "unbreakable umbrella"),
        )
        val mods = CurrentModifiers(
            state,
            modeOverrides = mapOf(Modeable.UMBRELLA to "bucket style"),
        )
        assertEquals(25.0, mods.values.get(DoubleModifier.ITEMDROP))
    }

    @Test
    fun umbrella_broken_givesMonsterLevelPercent() {
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.OFFHAND to "unbreakable umbrella"),
        )
        val mods = CurrentModifiers(
            state,
            modeOverrides = mapOf(Modeable.UMBRELLA to "broken"),
        )
        assertEquals(25.0, mods.values.get(DoubleModifier.MONSTER_LEVEL_PERCENT))
    }
}
