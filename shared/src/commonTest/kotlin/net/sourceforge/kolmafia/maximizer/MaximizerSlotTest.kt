package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaximizerSlotTest {

    @Test
    fun pseudoSlots_distinctFromRealAccessorySlots() {
        assertTrue(MaximizerSlot.WEAPON_1H.isPseudo)
        assertTrue(MaximizerSlot.OFFHAND_MELEE.isPseudo)
        assertTrue(MaximizerSlot.OFFHAND_RANGED.isPseudo)
        assertFalse(MaximizerSlot.ACC2.isPseudo)
        assertFalse(MaximizerSlot.ACC3.isPseudo)
        assertEquals(EquipmentSlot.ACC2, MaximizerSlot.ACC2.toEquipmentSlot())
        assertEquals(EquipmentSlot.WEAPON, MaximizerSlot.WEAPON_1H.toEquipmentSlot())
        assertEquals(EquipmentSlot.OFFHAND, MaximizerSlot.OFFHAND_MELEE.toEquipmentSlot())
    }

    @Test
    fun weaponBuckets_requireHands_includesPseudoWeaponSlot() {
        val spec = MaximizeSpec(
            primary = DoubleModifier.MUS,
            evaluator = Evaluator("muscle"),
            requireHands = true,
        )
        assertEquals(
            listOf(MaximizerSlot.WEAPON, MaximizerSlot.WEAPON_1H),
            MaximizerSlot.weaponBuckets(spec),
        )
    }

    @Test
    fun weaponBuckets_default_weaponOnly() {
        val spec = MaximizeSpec(
            primary = DoubleModifier.MUS,
            evaluator = Evaluator("muscle"),
        )
        assertEquals(listOf(MaximizerSlot.WEAPON), MaximizerSlot.weaponBuckets(spec))
    }

    @Test
    fun offhandBuckets_requireHands_includesPseudoOffhandSlots() {
        val spec = MaximizeSpec(
            primary = DoubleModifier.MUS,
            evaluator = Evaluator("muscle"),
            requireHands = true,
        )
        assertEquals(
            listOf(
                MaximizerSlot.OFFHAND,
                MaximizerSlot.OFFHAND_MELEE,
                MaximizerSlot.OFFHAND_RANGED,
            ),
            MaximizerSlot.offhandBuckets(spec),
        )
    }

    @Test
    fun offhandBuckets_default_offhandOnly() {
        val spec = MaximizeSpec(
            primary = DoubleModifier.MUS,
            evaluator = Evaluator("muscle"),
        )
        assertEquals(listOf(MaximizerSlot.OFFHAND), MaximizerSlot.offhandBuckets(spec))
    }

    @Test
    fun slotList_familiarBuckets_isolatedByIndex() {
        val list = SlotList<MaximizerRankedItem>(2)
        val itemA = MaximizerRankedItem(1, "hat-a", 10.0, MaximizerCheckedItem(1, "hat-a", initial = 1))
        val itemB = MaximizerRankedItem(2, "hat-b", 20.0, MaximizerCheckedItem(2, "hat-b", initial = 1))
        list.getFamiliar(0).add(itemA)
        list.getFamiliar(1).add(itemB)
        assertEquals(2, list.familiarCount())
        assertEquals(listOf(itemA), list.getFamiliar(0))
        assertEquals(listOf(itemB), list.getFamiliar(1))
    }

    @Test
    fun slotList_getSetAndSort() {
        val list = SlotList<MaximizerRankedItem>()
        val low = MaximizerRankedItem(1, "low", 1.0, MaximizerCheckedItem(1, "low", initial = 1))
        val high = MaximizerRankedItem(2, "high", 5.0, MaximizerCheckedItem(2, "high", initial = 1))
        list.get(MaximizerSlot.HAT).addAll(listOf(low, high))
        val sorted = list.sortedDescending(MaximizerSlot.HAT) { it.score }
        assertEquals(listOf(high, low), sorted)
    }
}
