package net.sourceforge.kolmafia.volcano

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VolcanoMapTest {

    @AfterTest
    fun tearDown() {
        VolcanoMapRng.resetRng()
    }

    @Test
    fun goalAndKnownPlatformAreOnBoard() {
        val coords = VolcanoMazeDatabaseTestHelper.sequenceOneLevelOne()
        val map = VolcanoMap(coords)
        assertTrue(map.inMap(VolcanoMazeConstants.GOAL))
        assertTrue(map.inMap(0))
    }

    @Test
    fun inMap_falseForLavaSquare() {
        val coords = VolcanoMazeDatabaseTestHelper.sequenceOneLevelOne()
        val map = VolcanoMap(coords)
        assertFalse(map.inMap(1))
    }

    @Test
    fun printMap_rendersPlatformsAndGoal() {
        val coords = VolcanoMazeDatabaseTestHelper.sequenceOneLevelOne()
        val map = VolcanoMap(coords)
        val lines = mutableListOf<String>()
        map.printMap(-1) { lines.add(it) }
        assertTrue(lines.any { it.contains('*') })
        assertTrue(lines.any { it.contains('O') })
    }
}

/** Loads level-1 map coordinates for sequence 1 from bundled data. */
internal object VolcanoMazeDatabaseTestHelper {
    fun sequenceOneLevelOne(): String =
        "0,4,7,9,21,27,31,37,45,51,53,62,66,68,78,85,86,96,107,112,113,116,118,121,127,131,133,137,138,143,159,161,163,166"
}
