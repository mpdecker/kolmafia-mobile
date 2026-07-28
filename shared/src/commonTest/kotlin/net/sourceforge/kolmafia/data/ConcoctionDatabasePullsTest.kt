package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcoctionDatabasePullsTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun getPullsRemaining_defaultsToNegativeOne() {
        ConcoctionDatabase.resetForTest()
        assertEquals(-1, ConcoctionDatabase.getPullsRemaining())
    }

    @Test
    fun setPullsRemaining_storesValue() {
        ConcoctionDatabase.setPullsRemaining(12)
        assertEquals(12, ConcoctionDatabase.getPullsRemaining())
    }

    @Test
    fun resetForTest_clearsPullsRemaining() {
        ConcoctionDatabase.setPullsRemaining(5)
        ConcoctionDatabase.resetForTest()
        assertEquals(-1, ConcoctionDatabase.getPullsRemaining())
    }
}
