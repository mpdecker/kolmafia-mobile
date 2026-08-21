package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.quest.SurvivorEncampmentChoiceSync

class GameRuntimeLibraryAshP884Test {
    @Test
    fun acceptedDonationConsumesSelectedFood() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(SurvivorEncampmentChoiceSync.apply(987, "choice.php?whichchoice=987&whichfood=123&giveten=1", "They accept your donation", { id, qty -> consumed += id to qty }))
        assertEquals(listOf(123 to 10), consumed)
        assertFalse(SurvivorEncampmentChoiceSync.apply(987, "choice.php?whichfood=123", "No thanks", { _, _ -> }))
    }
}
