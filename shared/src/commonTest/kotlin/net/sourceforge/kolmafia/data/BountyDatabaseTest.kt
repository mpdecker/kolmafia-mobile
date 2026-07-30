package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BountyDatabaseTest {

    @Test
    fun resolve_exactName() = runBlocking {
        BountyDatabase.load()
        assertEquals("bean-shaped rock", BountyDatabase.resolve("bean-shaped rock"))
    }

    @Test
    fun resolve_fuzzySingleMatch() = runBlocking {
        BountyDatabase.load()
        assertEquals("bean-shaped rock", BountyDatabase.resolve("bean-shaped"))
    }

    @Test
    fun resolve_unknownReturnsNull() = runBlocking {
        BountyDatabase.load()
        assertNull(BountyDatabase.resolve("xyzzy-not-a-bounty"))
    }

    @Test
    fun getMatchingNames_returnsCandidates() = runBlocking {
        BountyDatabase.load()
        val matches = BountyDatabase.getMatchingNames("bean")
        assertTrue(matches.contains("bean-shaped rock"))
    }

    @Test
    fun kolInternalType_easyHardSpecial() = runBlocking {
        BountyDatabase.load()
        assertEquals("low", BountyDatabase.getByName("bean-shaped rock")?.kolInternalType())
        assertEquals("high", BountyDatabase.getByName("absence of moss")?.kolInternalType())
    }

    @Test
    fun typeString_matchesBountyTxt() = runBlocking {
        BountyDatabase.load()
        assertEquals("easy", BountyDatabase.getByName("bean-shaped rock")?.typeString())
        assertEquals("hard", BountyDatabase.getByName("absence of moss")?.typeString())
    }
}
