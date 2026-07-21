package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CultShortsDatabaseTest {

    @AfterTest
    fun tearDown() {
        CultShortsDatabase.resetForTest()
    }

    @Test
    fun scrapPockets_sortedByScrapIndex() {
        val sample = """
            7	Scrap	3
            172	Scrap	5
            222	Scrap	2
            251	Scrap	6
            282	Scrap	7
            373	Scrap	1
            602	Scrap	4
            1	Stats	100	100	100
        """.trimIndent()
        assertEquals(
            listOf(373, 222, 7, 602, 172, 251, 282),
            CultShortsDatabase.parseForTest(sample),
        )
    }
}
