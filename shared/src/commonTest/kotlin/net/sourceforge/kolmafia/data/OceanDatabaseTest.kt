package net.sourceforge.kolmafia.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OceanDatabaseTest {

    @Test
    fun load_realFile_populatesDestinations() = runTest {
        OceanDatabase.resetForTest()
        OceanDatabase.load()

        assertTrue(OceanDatabase.isLoaded)
        assertTrue(OceanDatabase.loadedPointCount > 200)
        assertEquals(5, OceanDatabase.pointsFor(OceanDatabase.OceanDestination.GILLIGAN).size)
    }

    @Test
    fun pointsForKeyword_muscle_includesGilligan() = runTest {
        OceanDatabase.resetForTest()
        OceanDatabase.load()

        val muscle = OceanDatabase.pointsForKeyword("muscle")
        assertNotNull(muscle)
        assertTrue(OceanDatabase.OceanPoint(12, 84) in muscle)
    }

    @Test
    fun destinationAt_mainlandBlock() = runTest {
        OceanDatabase.resetForTest()
        OceanDatabase.load()

        val point = OceanDatabase.OceanPoint(12, 12)
        assertEquals(OceanDatabase.OceanDestination.MAINLAND, OceanDatabase.destinationAt(point))
        assertTrue(OceanDatabase.isMainland(point))
    }

    @Test
    fun parse_pointRoundTrip_plinth() = runTest {
        OceanDatabase.resetForTest()
        OceanDatabase.load()

        val point = OceanDatabase.OceanPoint.parse("63,29")
        assertNotNull(point)
        assertEquals(OceanDatabase.OceanDestination.PLINTH, OceanDatabase.destinationAt(point))
        assertEquals("63,29", point.toString())
    }

    @Test
    fun parse_skipsCommentsAndInvalid() {
        val snapshot = OceanDatabase.parseForTest(
            """
            1
            # comment row
            12	84	Gilligan's Island
            999	999	unknown place
            0	0	mainland
            """.trimIndent(),
        )

        assertEquals(1, snapshot.loadedPointCount)
        assertEquals(
            OceanDatabase.OceanDestination.GILLIGAN,
            snapshot.pointToDestination[OceanDatabase.OceanPoint(12, 84)],
        )
    }

    @Test
    fun oceanPoint_valid_bounds() {
        assertFalse(OceanDatabase.OceanPoint.valid(0, 0))
        assertFalse(OceanDatabase.OceanPoint.valid(243, 101))
        assertTrue(OceanDatabase.OceanPoint.valid(1, 1))
        assertTrue(OceanDatabase.OceanPoint.valid(242, 100))
        assertNull(OceanDatabase.OceanPoint.parse("0,0"))
        assertNull(OceanDatabase.OceanPoint.parse("243,101"))
    }

    @Test
    fun pointsForKeyword_unknown_returnsNull() = runTest {
        OceanDatabase.resetForTest()
        OceanDatabase.load()

        assertNull(OceanDatabase.pointsForKeyword("invalid"))
    }
}
