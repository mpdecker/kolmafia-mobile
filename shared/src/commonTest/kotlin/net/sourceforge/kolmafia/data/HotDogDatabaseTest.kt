package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HotDogDatabaseTest {

    @Test
    fun basicHotDog_isHotDogButNotFancy() {
        assertTrue(HotDogDatabase.isHotDog("basic hot dog"))
        assertFalse(HotDogDatabase.isFancyHotDog("basic hot dog"))
    }

    @Test
    fun fancyHotDogs_areFancy() {
        assertTrue(HotDogDatabase.isFancyHotDog("sly dog"))
        assertTrue(HotDogDatabase.isFancyHotDog("devil dog"))
        assertTrue(HotDogDatabase.isFancyHotDog("video games hot dog"))
    }

    @Test
    fun allEntries_matchDesktopOrderAndIds() {
        val expected = listOf(
            Triple("basic hot dog", -92, 1),
            Triple("savage macho dog", -93, 2),
            Triple("one with everything", -94, 2),
            Triple("sly dog", -95, 2),
            Triple("devil dog", -96, 3),
            Triple("chilly dog", -97, 3),
            Triple("ghost dog", -98, 3),
            Triple("junkyard dog", -99, 3),
            Triple("wet dog", -100, 3),
            Triple("optimal dog", -102, 1),
            Triple("sleeping dog", -101, 2),
            Triple("video games hot dog", -103, 3),
        )
        assertEquals(12, HotDogDatabase.entries.size)
        expected.forEachIndexed { index, (name, cafeId, fullness) ->
            assertTrue(HotDogDatabase.isHotDog(name), "missing hot dog: $name")
            assertEquals(index, HotDogDatabase.nameToIndex(name))
            assertEquals(name, HotDogDatabase.indexToName(index))
            assertEquals(cafeId, HotDogDatabase.indexToCafeId(index))
            assertEquals(fullness, HotDogDatabase.nameToFullness(name))
            assertEquals(index, HotDogDatabase.cafeIdToIndex(cafeId))
            assertEquals(name, HotDogDatabase.cafeIdToName(cafeId))
            if (index == 0) {
                assertFalse(HotDogDatabase.isFancyHotDog(name))
            } else {
                assertTrue(HotDogDatabase.isFancyHotDog(name))
            }
        }
    }

    @Test
    fun unknownName_returnsSentinels() {
        assertFalse(HotDogDatabase.isHotDog("not a hot dog"))
        assertFalse(HotDogDatabase.isFancyHotDog("not a hot dog"))
        assertEquals(-1, HotDogDatabase.nameToIndex("not a hot dog"))
        assertEquals(-1, HotDogDatabase.nameToFullness("not a hot dog"))
        assertEquals(-1, HotDogDatabase.cafeIdToIndex(9999))
        assertEquals(null, HotDogDatabase.cafeIdToName(9999))
    }

    @Test
    fun nameLookup_isCaseInsensitive() {
        assertTrue(HotDogDatabase.isHotDog("Sly Dog"))
        assertTrue(HotDogDatabase.isFancyHotDog("SLY DOG"))
        assertEquals(3, HotDogDatabase.nameToIndex("Sly Dog"))
    }

    @Test
    fun canQueueFancyDog_falseWhenAlreadyEatenOrQueued() {
        val openContext = ConcoctionQueueContext(
            getBooleanPref = { key -> key != "_fancyHotDogEaten" },
        )
        ConcoctionQueueBudget.queuedFancyDog = false
        assertTrue(HotDogDatabase.canQueueFancyDog(openContext))

        val eatenContext = ConcoctionQueueContext(
            getBooleanPref = { key -> key == "_fancyHotDogEaten" },
        )
        assertFalse(HotDogDatabase.canQueueFancyDog(eatenContext))

        ConcoctionQueueBudget.queuedFancyDog = true
        assertFalse(HotDogDatabase.canQueueFancyDog(openContext))
        ConcoctionQueueBudget.queuedFancyDog = false
    }
}
