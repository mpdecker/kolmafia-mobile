package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class CandyDatabaseTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        CandyDatabase.resetForTest()
    }

    @Test
    fun synthesisPair_trueWhenMatchingCandiesAvailable() {
        registerCandy(9100, "test candy a")
        registerCandy(9105, "test candy b")
        val available = CandyDatabase.synthesisPair(2165) { id ->
            when (id) {
                9100, 9105 -> 2
                else -> 0
            }
        }
        assertTrue(available)
    }

    @Test
    fun synthesisPair_falseWhenOnlyOneCandyAvailable() {
        registerCandy(9100, "test candy a")
        val available = CandyDatabase.synthesisPair(2165) { id ->
            when (id) {
                9100 -> 1
                else -> 0
            }
        }
        assertFalse(available)
    }

    @Test
    fun loadBlacklist_excludesBlacklistedPairing() {
        registerCandy(9100, "test candy a")
        registerCandy(9105, "blacklisted candy")
        val prefs = Preferences(MapSettings()).apply {
            setString("sweetSynthesisBlacklist", "blacklisted candy")
        }
        CandyDatabase.loadBlacklist(prefs)
        val available = CandyDatabase.synthesisPair(2165) { id ->
            when (id) {
                9100 -> 1
                9105 -> 2
                else -> 0
            }
        }
        assertFalse(available)
    }

    private fun registerCandy(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.POTION,
                secondaryUses = setOf("candy1"),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
