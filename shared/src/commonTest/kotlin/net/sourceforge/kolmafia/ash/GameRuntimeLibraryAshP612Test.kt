package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.quest.EquipmentDiscard
import net.sourceforge.kolmafia.quest.SneakyPeteDiscardSync

class GameRuntimeLibraryAshP612Test {

    @Test
    fun revision_phase612() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun sober_isNoOp() {
        val cleared = mutableListOf<EquipmentSlot>()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertFalse(
            SneakyPeteDiscardSync.applyFromAdventure(
                html = "You hand him your button and take his glowstick",
                inebriety = 25,
                equipment = mapOf(EquipmentSlot.ACC1 to "novelty button"),
                itemName = { if (it == SneakyPeteDiscardSync.NOVELTY_BUTTON) "novelty button" else "" },
                clearSlot = { cleared += it },
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertTrue(cleared.isEmpty())
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun overdrunk_button_discardsEquipped() {
        val cleared = mutableListOf<EquipmentSlot>()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            SneakyPeteDiscardSync.applyFromAdventure(
                html = "You hand him your button and take his glowstick",
                inebriety = 26,
                equipment = mapOf(EquipmentSlot.ACC1 to "novelty button"),
                itemName = { if (it == SneakyPeteDiscardSync.NOVELTY_BUTTON) "novelty button" else "" },
                clearSlot = { cleared += it },
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(EquipmentSlot.ACC1), cleared)
        assertEquals(listOf(SneakyPeteDiscardSync.NOVELTY_BUTTON to 1), consumed)
    }

    @Test
    fun overdrunk_crown_discardsEquipped() {
        val cleared = mutableListOf<EquipmentSlot>()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            SneakyPeteDiscardSync.applyFromAdventure(
                html = "Ah, man, you dropped your crown back there!",
                inebriety = 30,
                equipment = mapOf(EquipmentSlot.HAT to "tattered paper crown"),
                itemName = { if (it == SneakyPeteDiscardSync.TATTERED_PAPER_CROWN) "tattered paper crown" else "" },
                clearSlot = { cleared += it },
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(EquipmentSlot.HAT), cleared)
        assertEquals(listOf(SneakyPeteDiscardSync.TATTERED_PAPER_CROWN to 1), consumed)
    }

    @Test
    fun discardIfEquipped_skipsWhenNotWorn() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertFalse(
            EquipmentDiscard.discardIfEquipped(
                itemId = SneakyPeteDiscardSync.NOVELTY_BUTTON,
                equipment = mapOf(EquipmentSlot.HAT to "tattered paper crown"),
                itemName = { if (it == SneakyPeteDiscardSync.NOVELTY_BUTTON) "novelty button" else "" },
                clearSlot = {},
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertTrue(consumed.isEmpty())
    }
}
