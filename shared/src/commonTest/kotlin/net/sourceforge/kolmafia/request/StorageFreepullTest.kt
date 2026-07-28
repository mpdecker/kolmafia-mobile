package net.sourceforge.kolmafia.request

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase

class StorageFreepullTest {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
        ModifierDatabase.resetForTest()
    }

    @Test
    fun classifyContents_ronin_splitsFreepullFromStorage() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 3220,
                name = "hobo code binder",
                descId = "desc3220",
                image = "book2.gif",
                primaryUse = ItemPrimaryUse.OFFHAND,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest(
            "Item",
            "hobo code binder",
            "Free Pull",
        )
        val freepullId = 3220
        val regularId = 9999
        val ronin = CharacterState(isHardcore = true, roninLeft = 2)
        val contents = StoragePullRules.classifyContents(
            mapOf(freepullId to 1, regularId to 5),
            ronin,
        )
        assertEquals(1, contents.freepulls[freepullId])
        assertEquals(5, contents.storage[regularId])
    }

    @Test
    fun classifyContents_canInteract_keepsAllInStorage() {
        val contents = StoragePullRules.classifyContents(
            mapOf(100 to 3, 200 to 1),
            CharacterState(kingLiberated = true, roninLeft = 0),
        )
        assertEquals(3, contents.storage[100])
        assertEquals(1, contents.storage[200])
        assertEquals(0, contents.freepulls.size)
    }
}
