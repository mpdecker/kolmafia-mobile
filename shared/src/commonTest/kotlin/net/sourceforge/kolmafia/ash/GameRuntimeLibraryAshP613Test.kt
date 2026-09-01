package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.quest.TrickOrTreatSync

class GameRuntimeLibraryAshP613Test {

    @Test
    fun revision_phase613() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pumpkin_discardsMask() {
        val cleared = mutableListOf<EquipmentSlot>()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            TrickOrTreatSync.applyFromVisit(
                url = "trickortreat.php",
                html = "pull the pumpkin off of your head",
                equipment = mapOf(EquipmentSlot.HAT to "pumpkinhead mask"),
                itemName = names(),
                clearSlot = { cleared += it },
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(EquipmentSlot.HAT), cleared)
        assertEquals(listOf(TrickOrTreatSync.PUMPKINHEAD_MASK to 1), consumed)
    }

    @Test
    fun mummy_andWolfman_andGum() {
        val consumed = mutableListOf<Int>()
        assertTrue(
            TrickOrTreatSync.applyFromVisit(
                url = "trickortreat.php",
                html = "gick all over your mummy costume",
                equipment = mapOf(EquipmentSlot.SHIRT to "mummy costume"),
                itemName = names(),
                clearSlot = {},
                consumeItem = { id, _ -> consumed += id },
            ),
        )
        assertTrue(
            TrickOrTreatSync.applyFromVisit(
                url = "trickortreat.php",
                html = "unzipping the mask and throwing it behind you",
                equipment = mapOf(EquipmentSlot.HAT to "wolfman mask"),
                itemName = names(),
                clearSlot = {},
                consumeItem = { id, _ -> consumed += id },
            ),
        )
        assertTrue(
            TrickOrTreatSync.applyFromVisit(
                url = "trickortreat.php",
                html = "Right on, brah. Here, have some gum.",
                equipment = emptyMap(),
                itemName = names(),
                consumeItem = { id, _ -> consumed += id },
            ),
        )
        assertEquals(
            listOf(
                TrickOrTreatSync.MUMMY_COSTUME,
                TrickOrTreatSync.WOLFMAN_MASK,
                TrickOrTreatSync.RUSSIAN_ICE,
            ),
            consumed,
        )
    }

    @Test
    fun otherUrl_isNoOp() {
        assertFalse(
            TrickOrTreatSync.applyFromVisit(
                url = "adventure.php",
                html = "pull the pumpkin off of your head",
                equipment = mapOf(EquipmentSlot.HAT to "pumpkinhead mask"),
                itemName = names(),
            ),
        )
    }

    private fun names(): (Int) -> String = { id ->
        when (id) {
            TrickOrTreatSync.PUMPKINHEAD_MASK -> "pumpkinhead mask"
            TrickOrTreatSync.MUMMY_COSTUME -> "mummy costume"
            TrickOrTreatSync.WOLFMAN_MASK -> "wolfman mask"
            else -> ""
        }
    }
}
