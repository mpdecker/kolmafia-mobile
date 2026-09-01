package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.request.AscensionRecord

class AscensionSnapshotCacheTest {

    @Test
    fun filtersHardcoreAscensions() {
        val records = listOf(
            AscensionRecord(1, "Seal Clubber", "Hardcore", 100, 50),
            AscensionRecord(2, "Turtle Tamer", "None", 200, 60),
        )
        val snapshot = AscensionSnapshotCache.build(records, "Player", "1", AscensionSnapshotCache.Filter.HARDCORE)
        assertEquals(1, snapshot.ascensionCount)
        assertTrue(snapshot.records.first().pathName.contains("Hardcore"))
    }

    @Test
    fun extendedSummaryIncludesClassCounts() {
        val records = listOf(
            AscensionRecord(1, "Seal Clubber", "None", 100, 50),
            AscensionRecord(2, "Seal Clubber", "Hardcore", 200, 60),
        )
        val snapshot = AscensionSnapshotCache.build(records, "Player", "1")
        val lines = AscensionSnapshotCache.extendedSummaryLines(snapshot)
        assertTrue(lines.any { it.contains("Seal Clubber×2") })
        assertTrue(lines.any { it.contains("Best:") })
    }
}

class AscensionHistoryComparePointsTest {

    @Test
    fun compareReportsPointDeltas() {
        val previous = listOf(AscensionRecord(5, "Pastamancer", "None", 100, 10))
        val current = listOf(AscensionRecord(5, "Pastamancer", "None", 120, 15))
        val compare = AscensionHistoryCompareLogic.compare(previous, current)
        assertEquals(mapOf(5 to 5), compare.pointDeltas)
        assertTrue(compare.summaryLines().any { it.contains("points") })
    }
}
