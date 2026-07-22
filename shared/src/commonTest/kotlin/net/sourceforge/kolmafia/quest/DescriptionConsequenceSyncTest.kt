package net.sourceforge.kolmafia.quest

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.data.DescriptionConsequenceRegistry

class DescriptionConsequenceSyncTest {

    @AfterTest
    fun tearDown() {
        DescriptionConsequenceRegistry.resetForTest()
    }

    @Test
    fun pathForToday_usesInjectedCatalogAndDayDifference() {
        DescriptionConsequenceRegistry.injectForTest(
            listOf(
                "desc_item.php?whichitem=first",
                "desc_item.php?whichitem=second",
            ),
        )
        assertEquals(
            "desc_item.php?whichitem=second",
            DescriptionConsequenceSync.pathForToday(dayDifference = 1),
        )
    }

    @Test
    fun pathForToday_returnsNullWhenCatalogEmpty() {
        DescriptionConsequenceRegistry.injectForTest(emptyList())
        assertNull(DescriptionConsequenceSync.pathForToday(dayDifference = 0))
    }
}
