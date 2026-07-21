package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class PocketDatabaseTest {

    @BeforeTest
    fun setUp() = runTest {
        MonsterDatabase.load()
        EffectDatabase.load()
        ItemDatabase.load()
    }

    @AfterTest
    fun tearDown() {
        PocketDatabase.resetForTest()
    }

    @Test
    fun scrapPockets_sortedByScrapIndex() {
        val sample = """
            7	Scrap	3
            172	Scrap	5
            222	Scrap	2
            373	Scrap	1
            1	Stats	100	100	100
        """.trimIndent()
        PocketDatabase.applyParseForTest(PocketDatabase.parseForTest(sample))
        assertEquals(listOf(373, 222, 7, 172), PocketDatabase.scrapSyllables.map { it.pocket })
    }

    @Test
    fun monsterPocket_indexedByName() {
        val sample = """
            30	Monster	bookbat
            7	Scrap	3
        """.trimIndent()
        PocketDatabase.applyParseForTest(PocketDatabase.parseForTest(sample))
        val pocket = PocketDatabase.monsterPockets["bookbat"]
        assertNotNull(pocket)
        assertEquals(30, pocket.pocket)
        assertEquals("bookbat", pocket.monster.name)
        assertTrue(30 in PocketDatabase.allMonsterPockets)
    }

    @Test
    fun effectPocket_indexedByName() {
        val sample = """
            5	Effect	Super Vision (40)
        """.trimIndent()
        PocketDatabase.applyParseForTest(PocketDatabase.parseForTest(sample))
        val pockets = PocketDatabase.effectPockets["Super Vision"]
        assertNotNull(pockets)
        assertEquals(1, pockets.size)
        assertTrue(5 in PocketDatabase.allEffectPockets)
    }

    @Test
    fun itemPocket_indexedByName() {
        val sample = """
            27	Item	baconstone
        """.trimIndent()
        PocketDatabase.applyParseForTest(PocketDatabase.parseForTest(sample))
        val pockets = PocketDatabase.itemPockets["baconstone"]
        assertNotNull(pockets)
        assertEquals(1, pockets.size)
        assertTrue(27 in PocketDatabase.allItemPockets)
    }

    @Test
    fun firstUnpickedPocket_skipsPicked() {
        val sample = """
            5	Effect	Super Vision (40)
            15	Effect	Super Vision (40)
        """.trimIndent()
        PocketDatabase.applyParseForTest(PocketDatabase.parseForTest(sample))
        val sorted = PocketDatabase.sortResults("Super Vision", PocketDatabase.effectPockets["Super Vision"]!!)
        val first = PocketDatabase.firstUnpickedPocket(sorted, setOf(5))
        assertEquals(15, first?.pocket)
    }

    @Test
    fun fullBundledFile_loadsAllPockets() = runTest {
        PocketDatabase.load()
        assertTrue(PocketDatabase.allPockets.size >= 650, "loaded ${PocketDatabase.allPockets.size} pockets")
        assertEquals(7, PocketDatabase.scrapSyllables.size)
    }
}
