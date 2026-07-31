package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.preferences.Preferences

class TCRSDatabaseRefreshTest {

    private val itemId = 9999
    private val itemName = "tcrs refresh brew"

    @AfterTest
    fun tearDown() {
        TCRSDatabase.reset()
        ConcoctionDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ModifierDatabase.resetOverridesForTest()
        EffectDatabase.resetForTest()
    }

    @Test
    fun applyModifiers_endRefreshUpdatesConcoctionEffectName() {
        seedItemAndConcoction()
        ModifierDatabase.injectForTest("Item", itemName, "Effect: Bundled Effect")
        TCRSDatabase.injectMapForTest(
            mapOf(
                itemId to TCRSDatabase.TcrsEntry(
                    name = itemName,
                    size = 1,
                    quality = "good",
                    modifiers = "Effect: TCRS Effect",
                ),
            ),
        )

        TCRSDatabase.applyModifiers(characterLevel = 5)

        assertEquals("TCRS Effect", ConcoctionDatabase.getEffectName(itemName))
    }

    @Test
    fun resetModifiers_refreshBeforeVariableConsumables_restoresBundledEffect() {
        seedItemAndConcoction()
        ModifierDatabase.injectForTest("Item", itemName, "Effect: Bundled Effect")
        TCRSDatabase.injectMapForTest(
            mapOf(
                itemId to TCRSDatabase.TcrsEntry(
                    name = itemName,
                    size = 1,
                    quality = "good",
                    modifiers = "Effect: TCRS Effect",
                ),
            ),
        )
        TCRSDatabase.applyModifiers(characterLevel = 5)
        assertEquals("TCRS Effect", ConcoctionDatabase.getEffectName(itemName))

        val preferences = Preferences(MapSettings())
        TCRSDatabase.resetModifiers(preferences, characterLevel = 5)

        assertEquals("Bundled Effect", ConcoctionDatabase.getEffectName(itemName))
        assertTrue(ConcoctionDatabase.recalculateAdventureRangeForTest())
    }

    @Test
    fun resetModifiers_populatesAverageAdventureCache() = runBlocking {
        ConsumableDatabase.load()
        seedItemAndConcoction()
        ModifierDatabase.injectForTest("Item", itemName, "Effect: Bundled Effect")
        TCRSDatabase.injectMapForTest(
            mapOf(
                itemId to TCRSDatabase.TcrsEntry(
                    name = itemName,
                    size = 1,
                    quality = "good",
                    modifiers = "Effect: TCRS Effect",
                ),
            ),
        )

        val preferences = Preferences(MapSettings())
        preferences.setInt("smoresEaten", 1)
        TCRSDatabase.resetModifiers(preferences, characterLevel = 5)

        val expected = ceil(2.0.pow(1.75))
        assertEquals(expected, ConsumableDatabase.getAverageAdventures("s'more"))
    }

    private fun seedItemAndConcoction() {
        ItemDatabase.registerForTest(
            ItemData(
                id = itemId,
                name = itemName,
                descId = "tcrs_refresh_brew",
                image = "tcrsrefreshbrew.gif",
                primaryUse = ItemPrimaryUse.DRINK,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = itemName,
                resultQuantity = 1,
                methods = setOf("MIX"),
                ingredients = listOf(ConcoctionIngredient("olive oil", 1)),
            ),
        )
    }
}
