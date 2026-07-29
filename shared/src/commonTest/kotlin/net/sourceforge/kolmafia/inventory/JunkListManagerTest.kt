package net.sourceforge.kolmafia.inventory

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class JunkListManagerTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun load_seedsDefaultsWhenPrefEmpty() = runTest {
        registerItem(JUNK_A, "meat stack")
        registerItem(JUNK_B, "batgut")

        val prefs = Preferences(MapSettings())
        val manager = JunkListManager(GameDatabase())
        manager.load(prefs)

        assertTrue(prefs.getString(JunkListManager.PREF_KEY, "").contains("meat stack"))
        assertEquals(listOf(JUNK_A, JUNK_B), manager.itemIds())
        assertTrue(manager.contains(JUNK_A))
        assertFalse(manager.contains(9999))
    }

    @Test
    fun load_roundTripsPrefAndSkipsUnknownNames() = runTest {
        registerItem(JUNK_A, "meat stack")
        registerItem(JUNK_B, "batgut")

        val prefs = Preferences(MapSettings())
        prefs.setString(JunkListManager.PREF_KEY, "meat stack|missing item|batgut")

        val manager = JunkListManager(GameDatabase())
        manager.load(prefs)

        assertEquals(listOf(JUNK_A, JUNK_B), manager.itemIds())
    }

    @Test
    fun load_seedsSingletonAndMementoPrefs() = runTest {
        registerItem(SINGLETON_A, "bugbear beanie")
        registerItem(MEMENTO_A, "tiny plastic Crimbo wreath")

        val prefs = Preferences(MapSettings())
        val manager = JunkListManager(GameDatabase())
        manager.load(prefs)

        assertTrue(prefs.getString(JunkListManager.SINGLETON_PREF_KEY, "").contains("bugbear beanie"))
        assertTrue(prefs.getString(JunkListManager.MEMENTO_PREF_KEY, "").contains("tiny plastic Crimbo wreath"))
        assertTrue(manager.isSingleton(SINGLETON_A))
        assertTrue(manager.isMemento(MEMENTO_A))
    }

    @Test
    fun load_mergesSingletonIntoJunkIds() = runTest {
        registerItem(JUNK_A, "meat stack")
        registerItem(SINGLETON_A, "potted sporeling")

        val prefs = Preferences(MapSettings())
        prefs.setString(JunkListManager.PREF_KEY, "meat stack")
        prefs.setString(JunkListManager.SINGLETON_PREF_KEY, "potted sporeling")

        val manager = JunkListManager(GameDatabase())
        manager.load(prefs)

        assertEquals(listOf(JUNK_A, SINGLETON_A), manager.itemIds())
        assertTrue(manager.contains(SINGLETON_A))
    }

    @Test
    fun addToJunkList_persistsPref() = runTest {
        registerItem(NEW_JUNK, "ronin smash item")

        val prefs = Preferences(MapSettings())
        val manager = JunkListManager(GameDatabase())
        manager.load(prefs)
        manager.addToJunkList(NEW_JUNK)

        assertTrue(manager.contains(NEW_JUNK))
        assertTrue(prefs.getString(JunkListManager.PREF_KEY, "").contains("ronin smash item"))
    }

    @Test
    fun load_profitableListEmptyByDefault() = runTest {
        registerItem(JUNK_A, "meat stack")

        val prefs = Preferences(MapSettings())
        val manager = JunkListManager(GameDatabase())
        manager.load(prefs)

        assertEquals(emptyList(), manager.profitableIds())
        assertEquals("", prefs.getString(JunkListManager.PROFITABLE_PREF_KEY, ""))
    }

    @Test
    fun importItemFlags_parsesAllSectionsAndMergesSingletonIntoJunk() = runTest {
        registerItem(JUNK_A, "meat stack")
        registerItem(SINGLETON_A, "bugbear beanie")
        registerItem(MEMENTO_A, "tiny plastic Crimbo wreath")
        registerItem(PROFITABLE_A, "seal tooth")

        val prefs = Preferences(MapSettings())
        val manager = JunkListManager(GameDatabase())
        manager.load(prefs)

        manager.importItemFlags(
            """
             > junk
            meat stack

             > singleton
            bugbear beanie

             > mementos
            tiny plastic Crimbo wreath

             > profitable
            2 seal tooth
            """.trimIndent(),
        )

        assertTrue(manager.isProfitable(PROFITABLE_A))
        assertTrue(manager.isSingleton(SINGLETON_A))
        assertTrue(manager.isMemento(MEMENTO_A))
        assertTrue(manager.contains(SINGLETON_A))
        assertTrue(prefs.getBoolean(JunkListManager.ITEM_FLAGS_IMPORTED_KEY, false))
    }

    @Test
    fun exportItemFlags_roundTripsThroughImport() = runTest {
        registerItem(JUNK_A, "meat stack")
        registerItem(SINGLETON_A, "bugbear beanie")
        registerItem(MEMENTO_A, "tiny plastic Crimbo wreath")
        registerItem(PROFITABLE_A, "seal tooth")

        val prefs = Preferences(MapSettings())
        val manager = JunkListManager(GameDatabase())
        manager.load(prefs)
        manager.importItemFlags(
            """
             > junk
            meat stack

             > singleton
            bugbear beanie

             > mementos
            tiny plastic Crimbo wreath

             > profitable
            seal tooth
            """.trimIndent(),
        )

        val exported = manager.exportItemFlags()
        manager.importItemFlags(exported)

        assertEquals(listOf(JUNK_A, SINGLETON_A), manager.itemIds())
        assertEquals(listOf(SINGLETON_A), manager.singletonIds())
        assertEquals(listOf(MEMENTO_A), manager.mementoIds())
        assertEquals(listOf(PROFITABLE_A), manager.profitableIds())
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val JUNK_A = 100
        private const val JUNK_B = 101
        private const val SINGLETON_A = 102
        private const val MEMENTO_A = 103
        private const val NEW_JUNK = 104
        private const val PROFITABLE_A = 105
    }
}
