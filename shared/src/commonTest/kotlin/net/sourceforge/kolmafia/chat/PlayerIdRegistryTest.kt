package net.sourceforge.kolmafia.chat

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerIdRegistryTest {

    @AfterTest
    fun tearDown() {
        PlayerIdRegistry.clearForTest()
    }

    @Test
    fun registerAndGetPlayerId_returnsCachedId() {
        PlayerIdRegistry.register("Onlyfax", "12345")
        assertEquals("12345", PlayerIdRegistry.getPlayerId("onlyfax"))
    }

    @Test
    fun register_skipsNegativeIds() {
        PlayerIdRegistry.register("Ghost", "-1")
        assertEquals("Ghost", PlayerIdRegistry.getPlayerId("Ghost"))
    }

    @Test
    fun getPlayerId_withRetrieveId_invokesLookup() {
        var lookedUp = false
        val result = PlayerIdRegistry.getPlayerId("onlyfax", retrieveId = true) {
            lookedUp = true
            PlayerIdRegistry.register("onlyfax", "999")
        }
        assertTrue(lookedUp)
        assertEquals("999", result)
    }

    @Test
    fun getPlayerId_unknownWithoutLookup_returnsName() {
        assertEquals("unknown", PlayerIdRegistry.getPlayerId("unknown"))
    }

    @Test
    fun getPlayerName_returnsCachedName() {
        PlayerIdRegistry.register("Easyfax", "67890")
        assertEquals("Easyfax", PlayerIdRegistry.getPlayerName("67890"))
    }

    @Test
    fun getPlayerName_unknownWithoutLookup_returnsId() {
        assertEquals("42", PlayerIdRegistry.getPlayerName("42"))
    }
}
