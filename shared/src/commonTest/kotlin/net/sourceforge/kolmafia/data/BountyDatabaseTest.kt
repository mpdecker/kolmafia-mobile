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
}
