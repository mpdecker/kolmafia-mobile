package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NpcStoreDatabaseTest {

    private val sampleText = """
2
Shadowy Store	guildstore1	ye olde golde frontes	1500	ROW522
Shadowy Store	guildstore1	pretentious paintbrush	1000	ROW523
General Store	generalstore	a beret	500	ROW100
General Store	generalstore	BORT license plate	99	ROW101
""".trimIndent()

    @BeforeTest
    fun setUp() {
        NpcStoreDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        NpcStoreDatabase.resetForTest()
    }

    @Test
    fun `npcPrice returns correct price for known item`() {
        NpcStoreDatabase.loadFromText(sampleText)
        assertEquals(1500, NpcStoreDatabase.npcPrice("ye olde golde frontes"))
        assertEquals(1000, NpcStoreDatabase.npcPrice("pretentious paintbrush"))
        assertEquals(500, NpcStoreDatabase.npcPrice("a beret"))
        assertEquals(99, NpcStoreDatabase.npcPrice("BORT license plate"))
    }

    @Test
    fun `npcPrice returns 0 for unknown item`() {
        NpcStoreDatabase.loadFromText(sampleText)
        assertEquals(0, NpcStoreDatabase.npcPrice("nonexistent item"))
        assertEquals(0, NpcStoreDatabase.npcPrice(""))
    }

    @Test
    fun `npcPrice is case-insensitive`() {
        NpcStoreDatabase.loadFromText(sampleText)
        assertEquals(1500, NpcStoreDatabase.npcPrice("YE OLDE GOLDE FRONTES"))
        assertEquals(1500, NpcStoreDatabase.npcPrice("Ye Olde Golde Frontes"))
        assertEquals(99, NpcStoreDatabase.npcPrice("bort license plate"))
    }

    @Test
    fun `stores are parsed with correct storeName and storeKey`() {
        NpcStoreDatabase.loadFromText(sampleText)

        val store = NpcStoreDatabase.getByKey("guildstore1")
        assertNotNull(store, "Expected store with key guildstore1 to exist")
        assertEquals("guildstore1", store.storeKey)
        assertEquals("Shadowy Store", store.storeName)

        val store2 = NpcStoreDatabase.getByKey("generalstore")
        assertNotNull(store2, "Expected store with key generalstore to exist")
        assertEquals("generalstore", store2.storeKey)
        assertEquals("General Store", store2.storeName)
    }

    @Test
    fun `all stores from npcstores txt format are NPC stores`() {
        NpcStoreDatabase.loadFromText(sampleText)

        assertTrue(NpcStoreDatabase.npcStores().isNotEmpty(), "Expected npcStores() to be non-empty")
        assertTrue(NpcStoreDatabase.coinmasters().isEmpty(), "Expected coinmasters() to be empty for NPC-only file")
    }

    @Test
    fun `stores have items attached`() {
        NpcStoreDatabase.loadFromText(sampleText)

        val store = NpcStoreDatabase.getByKey("guildstore1")
        assertNotNull(store)
        assertEquals(2, store.items.size)
        assertTrue(store.items.any { it.itemName == "ye olde golde frontes" && it.price == 1500 })
        assertTrue(store.items.any { it.itemName == "pretentious paintbrush" && it.price == 1000 })
    }

    @Test
    fun `version number line is skipped`() {
        NpcStoreDatabase.loadFromText(sampleText)
        // If version number "2" was parsed as a store key, it would show up in all()
        // Verify no store with key "2" or storeName "2" was created
        assertEquals(null, NpcStoreDatabase.getByKey("2"))
    }

    @Test
    fun `duplicate item names keep first price`() {
        val textWithDupe = """
1
Store A	storea	magic widget	100	ROW1
Store B	storeb	magic widget	200	ROW2
""".trimIndent()
        NpcStoreDatabase.loadFromText(textWithDupe)
        assertEquals(100, NpcStoreDatabase.npcPrice("magic widget"))
    }

    @Test
    fun `storeForItem returns the store that sells the item`() {
        NpcStoreDatabase.loadFromText(sampleText)
        val store = NpcStoreDatabase.storeForItem("ye olde golde frontes")
        assertNotNull(store)
        assertEquals("guildstore1", store!!.storeKey)
    }

    @Test
    fun `storeForItem is case-insensitive`() {
        NpcStoreDatabase.loadFromText(sampleText)
        val store = NpcStoreDatabase.storeForItem("YE OLDE GOLDE FRONTES")
        assertNotNull(store)
        assertEquals("guildstore1", store!!.storeKey)
    }

    @Test
    fun `storeForItem returns null for unknown item`() {
        NpcStoreDatabase.loadFromText(sampleText)
        assertNull(NpcStoreDatabase.storeForItem("imaginary widget"))
    }

    @Test
    fun `storeForItem returns null after resetForTest`() {
        NpcStoreDatabase.loadFromText(sampleText)
        NpcStoreDatabase.resetForTest()
        assertNull(NpcStoreDatabase.storeForItem("ye olde golde frontes"))
    }
}
