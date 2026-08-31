package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

class RequestRoutingResidualTest {

    @Test
    fun absoluteTeaTreeResponseIsRoutedIdempotently() {
        val preferences = Preferences(MapSettings())
        val library = GameRuntimeLibrary(preferences = preferences)
        val url =
            "https://www.kingdomofloathing.com/choice.php?whichchoice=1104&option=1"

        library.processVisitResponseHooks("You shake the tree.", url)
        library.processVisitResponseHooks("You shake the tree.", url)

        assertTrue(preferences.getBoolean("_pottedTeaTreeUsed", false))
    }

    @Test
    fun relativeSpecificTeaResponseIsRoutedIdempotently() {
        val preferences = Preferences(MapSettings())
        val library = GameRuntimeLibrary(preferences = preferences)
        val url = "/choice.php?whichchoice=1105&option=1&itemid=123"

        library.processVisitResponseHooks("You harvest a specific tea.", url)
        library.processVisitResponseHooks("You harvest a specific tea.", url)

        assertTrue(preferences.getBoolean("_pottedTeaTreeUsed", false))
    }

    @Test
    fun equivalentAbsoluteAndRelativeHashingResponsesConsumeOnce() {
        val inventory = inventoryWith(HASHABLE_ITEM_ID, 2)
        val library = GameRuntimeLibrary(inventoryManager = inventory)
        val html = "You crush the schematic into little bits of checksum."
        val relative = "choice.php?whichchoice=1551&option=1&iid=$HASHABLE_ITEM_ID"
        val absolute = "https://www.kingdomofloathing.com/$relative"

        library.processVisitResponseHooks(html, absolute)
        library.processVisitResponseHooks(html, relative)

        assertEquals(1, inventory.getCount(HASHABLE_ITEM_ID))
    }

    @Test
    fun malformedChoiceUrlDoesNotMutateState() {
        val preferences = Preferences(MapSettings())
        val inventory = inventoryWith(HASHABLE_ITEM_ID, 2)
        val library = GameRuntimeLibrary(
            preferences = preferences,
            inventoryManager = inventory,
        )

        library.processVisitResponseHooks(
            "You crush the schematic into little bits of checksum.",
            "/choice.php?whichchoice=not-a-number&option=1&iid=$HASHABLE_ITEM_ID",
        )

        assertFalse(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(2, inventory.getCount(HASHABLE_ITEM_ID))
    }

    @Test
    fun requestLoggerNamesResidualChoicesCorrectly() {
        val preferences = Preferences(MapSettings())
        val logger = SessionLogger(preferences, GameEventBus())
        RequestLogger.currentRound = { 0 }
        ChoiceCombatAshState.reset()

        RequestLogger.registerRequest(
            "choice.php?whichchoice=1551&option=1&iid=$HASHABLE_ITEM_ID",
            logger,
            preferences,
        )
        assertEquals("Hashing Vise", logger.recentLines().last())

        RequestLogger.registerRequest(
            "choice.php?whichchoice=1104&option=1",
            logger,
            preferences,
        )
        assertEquals("Tea Tree", logger.recentLines().last())

        RequestLogger.registerRequest(
            "choice.php?whichchoice=1105&option=1&itemid=123",
            logger,
            preferences,
        )
        assertEquals("Tea Tree", logger.recentLines().last())
    }

    private fun inventoryWith(itemId: Int, quantity: Int): InventoryManager =
        InventoryManager(
            HttpClient(MockEngine { respond("{}") }),
            GameEventBus(),
        ).also {
            it.applyParsedInventory(
                mapOf(itemId to InventoryItem(itemId, "test schematic", quantity, ItemType.OTHER)),
            )
        }

    companion object {
        private const val HASHABLE_ITEM_ID = 11999
    }
}
