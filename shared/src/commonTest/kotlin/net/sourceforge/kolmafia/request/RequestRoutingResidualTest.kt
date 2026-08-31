package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
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
        val settings = CountingSettings()
        val preferences = Preferences(settings)
        val library = GameRuntimeLibrary(preferences = preferences)
        val url =
            "https://www.kingdomofloathing.com/choice.php?whichchoice=1104&option=1"

        library.processVisitResponseHooks(TEA_SUCCESS_HTML, url)
        library.processVisitResponseHooks(TEA_SUCCESS_HTML, url)

        assertTrue(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(1, settings.teaTreeWrites)
    }

    @Test
    fun relativeSpecificTeaResponseIsRoutedIdempotently() {
        val settings = CountingSettings()
        val preferences = Preferences(settings)
        val library = GameRuntimeLibrary(preferences = preferences)
        val url = "/choice.php?whichchoice=1105&option=1&itemid=123"

        library.processVisitResponseHooks(TEA_SUCCESS_HTML, url)
        library.processVisitResponseHooks(TEA_SUCCESS_HTML, url)

        assertTrue(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(1, settings.teaTreeWrites)
    }

    @Test
    fun handledSignaturesRemainDeduplicatedAcrossInterveningResponses() {
        val settings = CountingSettings()
        val library = GameRuntimeLibrary(preferences = Preferences(settings))
        val first = "choice.php?whichchoice=1104&option=1"
        val second = "choice.php?whichchoice=1105&option=1&itemid=123"

        library.processVisitResponseHooks(TEA_SUCCESS_HTML, first)
        library.processVisitResponseHooks(TEA_SUCCESS_HTML, second)
        library.processVisitResponseHooks(TEA_SUCCESS_HTML, first)

        assertEquals(2, settings.teaTreeWrites)
    }

    @Test
    fun requestBoundaryPermitsLaterIdenticalHandledResponse() {
        val settings = CountingSettings()
        val library = GameRuntimeLibrary(preferences = Preferences(settings))
        val url = "choice.php?whichchoice=1104&option=1"

        library.processVisitResponseHooks(TEA_SUCCESS_HTML, url)
        library.processVisitResponseHooks(TEA_SUCCESS_HTML, url)
        library.resetVisitResponseHookSignatures()
        library.processVisitResponseHooks(TEA_SUCCESS_HTML, url)

        assertEquals(2, settings.teaTreeWrites)
    }

    @Test
    fun failedTeaTreeResponsesDoNotMarkDailyUse() {
        val settings = CountingSettings()
        val preferences = Preferences(settings)
        val library = GameRuntimeLibrary(preferences = preferences)

        library.processVisitResponseHooks(
            "You have already harvested your potted tea tree today.",
            "choice.php?whichchoice=1104&option=1",
        )
        library.processVisitResponseHooks(
            "<html>Malformed response without an acquisition.</html>",
            "choice.php?whichchoice=1105&option=1&itemid=123",
        )

        assertFalse(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(0, settings.teaTreeWrites)
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

    private class CountingSettings(
        private val delegate: Settings = MapSettings(),
    ) : Settings by delegate {
        var teaTreeWrites: Int = 0
            private set

        override fun putBoolean(key: String, value: Boolean) {
            if (key == "_pottedTeaTreeUsed") {
                teaTreeWrites++
            }
            delegate.putBoolean(key, value)
        }
    }

    companion object {
        private const val HASHABLE_ITEM_ID = 11999
        private const val TEA_SUCCESS_HTML = "You acquire an item: a delicious cup of tea."
    }
}
