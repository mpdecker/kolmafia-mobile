package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VolcanoMazeDatabaseTest {

    @AfterTest
    fun tearDown() {
        VolcanoMazeDatabase.resetForTest()
    }

    @Test
    fun load_readsAllMapSequences() = runBlocking {
        VolcanoMazeDatabase.load()
        assertTrue(VolcanoMazeDatabase.isLoaded)
        assertEquals(6, VolcanoMazeDatabase.loadedSequenceCount)
        for (key in 1..6) {
            val sequence = VolcanoMazeDatabase.getMapSequence(key)
            assertNotNull(sequence)
            assertEquals(5, sequence.size)
            assertTrue(sequence.all { it != null })
        }
    }

    @Test
    fun getMapSequence_levelOnePlatformCount() = runBlocking {
        VolcanoMazeDatabase.load()
        val map = VolcanoMazeDatabase.getMapSequence(1)?.get(0)
        assertNotNull(map)
        assertEquals(34, map.platforms.size)
    }

    @Test
    fun keyForCoordinates_roundTripsKnownSequence() = runBlocking {
        VolcanoMazeDatabase.load()
        val coords = VolcanoMazeDatabase.getMapSequence(1)?.get(0)?.coordinates
        assertNotNull(coords)
        assertEquals(1, VolcanoMazeDatabase.keyForCoordinates(coords))
    }
}
