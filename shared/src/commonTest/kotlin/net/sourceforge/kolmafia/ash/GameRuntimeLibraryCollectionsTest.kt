package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryCollectionsTest {

    @Test
    fun getInventory_emptyWhenNoInventoryManager() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(count(get_inventory())));"))
    }

    @Test
    fun getCloset_returnsEmptyAggregate() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(count(get_closet())));"))
    }

    @Test
    fun getStorage_returnsEmptyAggregate() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(count(get_storage())));"))
    }

    @Test
    fun getStash_returnsEmptyAggregate() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(count(get_stash())));"))
    }

    @Test
    fun getDisplay_returnsEmptyAggregate() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(count(get_display())));"))
    }
}
