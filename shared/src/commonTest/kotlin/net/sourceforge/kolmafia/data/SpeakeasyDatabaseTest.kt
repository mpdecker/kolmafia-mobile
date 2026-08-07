package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeakeasyDatabaseTest {

    @Test
    fun allEntries_matchDesktopSpeakeasyDrinks() {
        val expected = listOf(
            Triple("glass of &quot;milk&quot;", 7589, 1 to 250),
            Triple("cup of &quot;tea&quot;", 7590, 1 to 250),
            Triple("thermos of &quot;whiskey&quot;", 7591, 1 to 250),
            Triple("Lucky Lindy", 7592, 1 to 500),
            Triple("Bee's Knees", 7593, 2 to 500),
            Triple("Sockdollager", 7594, 2 to 500),
            Triple("Ish Kabibble", 7595, 2 to 500),
            Triple("Hot Socks", 7596, 3 to 5000),
            Triple("Phonus Balonus", 7597, 3 to 10000),
            Triple("Flivver", 7598, 2 to 20000),
            Triple("Sloppy Jalopy", 7599, 5 to 100000),
        )
        assertEquals(11, SpeakeasyDatabase.entries.size)
        expected.forEachIndexed { index, (name, itemId, stats) ->
            val (inebriety, cost) = stats
            assertTrue(SpeakeasyDatabase.isSpeakeasyDrink(name), "missing drink: $name")
            assertTrue(SpeakeasyDatabase.isSpeakeasyDrink(itemId))
            assertEquals(index, SpeakeasyDatabase.nameToIndex(name))
            assertEquals(name, SpeakeasyDatabase.indexToName(index))
            assertEquals(cost, SpeakeasyDatabase.nameToCost(name))
            assertEquals(inebriety, SpeakeasyDatabase.nameToInebriety(name))
        }
    }

    @Test
    fun canQueueSpeakeasyDrink_respectsDailyCap() {
        val context = ConcoctionQueueContext(
            getIntPref = { key ->
                when (key) {
                    "_speakeasyDrinksDrunk" -> 0
                    else -> 0
                }
            },
        )
        assertTrue(SpeakeasyDatabase.canQueueSpeakeasyDrink(1, context))
        assertTrue(SpeakeasyDatabase.canQueueSpeakeasyDrink(3, context))

        val atCap = ConcoctionQueueContext(getIntPref = { if (it == "_speakeasyDrinksDrunk") 3 else 0 })
        assertFalse(SpeakeasyDatabase.canQueueSpeakeasyDrink(1, atCap))

        val queuedWouldExceed = ConcoctionQueueContext(
            getIntPref = { if (it == "_speakeasyDrinksDrunk") 2 else 0 },
        )
        ConcoctionQueueBudget.queuedSpeakeasyDrink = 0
        assertFalse(SpeakeasyDatabase.canQueueSpeakeasyDrink(2, queuedWouldExceed))
        assertTrue(SpeakeasyDatabase.canQueueSpeakeasyDrink(1, queuedWouldExceed))
    }

    @Test
    fun canQueueSpeakeasyDrink_countsQueuedDrinks() {
        ConcoctionQueueBudget.queuedSpeakeasyDrink = 2
        val context = ConcoctionQueueContext(getIntPref = { if (it == "_speakeasyDrinksDrunk") 0 else 0 })
        assertTrue(SpeakeasyDatabase.canQueueSpeakeasyDrink(1, context))
        assertFalse(SpeakeasyDatabase.canQueueSpeakeasyDrink(2, context))
        ConcoctionQueueBudget.queuedSpeakeasyDrink = 0
    }
}
