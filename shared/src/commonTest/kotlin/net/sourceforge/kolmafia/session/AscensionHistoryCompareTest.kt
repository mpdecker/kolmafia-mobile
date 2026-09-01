package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.request.AscensionRecord

class AscensionHistoryCompareTest {
    @Test
    fun compare_detectsNewAscensionAndTurnDelta() {
        val previous = listOf(
            AscensionRecord(182, "Seal Clubber", "Avatar of Boris", 469, 2),
            AscensionRecord(181, "Sauceror", "None", 512, 1),
        )
        val current = listOf(
            AscensionRecord(183, "Pastamancer", "Zombie Slayer", 400, 1),
            AscensionRecord(182, "Seal Clubber", "Avatar of Boris", 500, 2),
            AscensionRecord(181, "Sauceror", "None", 512, 1),
        )

        val compare = AscensionHistoryCompareLogic.compare(previous, current)

        assertEquals(1, compare.newAscensions.size)
        assertEquals(183, compare.newAscensions.single().number)
        assertEquals(mapOf(182 to 31), compare.turnDeltas)
        assertTrue(compare.hasChanges)
        assertEquals(2, compare.summaryLines().size)
    }

    @Test
    fun compare_emptyWhenNoPreviousSnapshot() {
        val compare = AscensionHistoryCompareLogic.compare(emptyList(), listOf(
            AscensionRecord(1, "Turtle Tamer", "None", 100, 1),
        ))
        assertFalse(compare.hasChanges)
    }
}
