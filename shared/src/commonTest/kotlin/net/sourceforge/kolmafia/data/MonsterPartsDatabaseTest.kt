package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonsterPartsDatabaseTest {

    @AfterTest
    fun tearDown() {
        MonsterPartsDatabase.resetForTest()
    }

    @Test
    fun load_readsMonsterParts() = runBlocking {
        MonsterPartsDatabase.load()
        assertTrue(MonsterPartsDatabase.isLoaded)
        assertTrue(MonsterPartsDatabase.loadedEntryCount > 100)
        assertEquals(
            listOf("arm", "head", "leg", "torso"),
            MonsterPartsDatabase.partsForId(1),
        )
    }

    @Test
    fun partsForId_missingId_returnsEmpty() = runBlocking {
        MonsterPartsDatabase.load()
        assertEquals(emptyList(), MonsterPartsDatabase.partsForId(-999))
    }

    @Test
    fun parseForTest_skipsNameColumn() {
        val parsed = MonsterPartsDatabase.parseForTest(
            """
            1
            # comment
            1	spooky vampire	arm	head	leg	torso
            3	dodecapede	head	leg	torso
            """.trimIndent(),
        )
        assertEquals(listOf("arm", "head", "leg", "torso"), parsed[1])
        assertEquals(listOf("head", "leg", "torso"), parsed[3])
    }
}
