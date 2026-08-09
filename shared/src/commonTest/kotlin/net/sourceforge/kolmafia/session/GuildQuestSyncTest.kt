package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemRules
import net.sourceforge.kolmafia.quest.QuestLogSync

class GuildQuestSyncTest {

    private fun prefs() = Preferences(MapSettings())

    private fun character(ascension: Int = 5): KoLCharacter =
        KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(ascensions = ascension.toString()),
            )
        }

    private fun inventoryWithEnvelope(quantity: Int = 1): InventoryManager {
        val envelopeId = QuestLogSync.FACTORY_ENVELOPE_ID
        return object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            init {
                _state.value = InventoryState(
                    items = mapOf(
                        envelopeId to InventoryItem(
                            envelopeId,
                            "thick padded envelope",
                            quantity,
                            ItemType.OTHER,
                        ),
                    ),
                )
            }
        }
    }

    private fun inventoryWithItem(itemId: Int, name: String, quantity: Int = 1): InventoryManager =
        object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            init {
                _state.value = InventoryState(
                    items = mapOf(
                        itemId to InventoryItem(itemId, name, quantity, ItemType.OTHER),
                    ),
                )
            }
        }

    @Test
    fun applyPlaceVisit_pacoSouthOfTheBorder_setsLastDesertUnlock() {
        val prefs = prefs()

        GuildQuestSync.applyPlaceVisit(
            place = "paco",
            html = "Welcome South of the Border!",
            character = character(ascension = 7),
            preferences = prefs,
            questDatabase = null,
            inventoryManager = null,
            sessionLogger = null,
        )

        assertEquals(7, prefs.getInt("lastDesertUnlock", -1))
    }

    @Test
    fun applyPlaceVisit_pacoWithEnvelope_finishesFactoryAndConsumes() {
        val prefs = prefs()
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FACTORY, QuestDatabase.STARTED)
        val inventory = inventoryWithEnvelope()

        GuildQuestSync.applyPlaceVisit(
            place = "paco",
            html = "Thanks for the delivery.",
            character = character(),
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventory,
            sessionLogger = null,
        )

        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.FACTORY))
        assertNull(inventory.state.value.items[QuestLogSync.FACTORY_ENVELOPE_ID])
    }

    @Test
    fun applyPlaceVisit_pacoWithoutEnvelope_noFactoryFinish() {
        val prefs = prefs()
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FACTORY, QuestDatabase.STARTED)
        val inventory = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {}

        GuildQuestSync.applyPlaceVisit(
            place = "paco",
            html = "Hello paco.",
            character = character(),
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventory,
            sessionLogger = null,
        )

        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.FACTORY))
    }

    @Test
    fun applyPlaceVisit_wrongPlace_noOp() {
        val prefs = prefs()

        GuildQuestSync.applyPlaceVisit(
            place = "ocg",
            html = "South of the Border",
            character = character(ascension = 7),
            preferences = prefs,
            questDatabase = null,
            inventoryManager = null,
            sessionLogger = null,
        )

        assertEquals(-1, prefs.getInt("lastDesertUnlock", -1))
    }

    @Test
    fun applyPlaceVisit_ocgWithKeyAndTurnInHtml_advancesEgoAndConsumesKey() {
        val prefs = prefs()
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.EGO, QuestDatabase.STARTED)
        val inventory = inventoryWithItem(QuestLogSync.FERNSWARTHY_KEY_ID, "Fernswarthy's key")

        GuildQuestSync.applyPlaceVisit(
            place = "ocg",
            html = "grins and takes Fernswarthy's key from you.",
            character = character(),
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventory,
            sessionLogger = null,
        )

        assertEquals("step1", db.getProgress(Quest.EGO))
        assertNull(inventory.state.value.items[QuestLogSync.FERNSWARTHY_KEY_ID])
    }

    @Test
    fun applyPlaceVisit_ocgWithoutTurnInHtml_doesNotConsumeKey() {
        val prefs = prefs()
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.EGO, QuestDatabase.STARTED)
        val inventory = inventoryWithItem(QuestLogSync.FERNSWARTHY_KEY_ID, "Fernswarthy's key")

        GuildQuestSync.applyPlaceVisit(
            place = "ocg",
            html = "Welcome to the Other Class Guild.",
            character = character(),
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventory,
            sessionLogger = null,
        )

        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.EGO))
        assertEquals(1, inventory.state.value.items[QuestLogSync.FERNSWARTHY_KEY_ID]?.quantity)
    }

    @Test
    fun applyPlaceVisit_challengeSausage_finishesMuscleAndConsumes() {
        val prefs = prefs()
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.MUSCLE, QuestDatabase.STARTED)
        val inventory = inventoryWithItem(QuestItemRules.BIG_KNOB_SAUSAGE_ID, "big knob sausage")

        GuildQuestSync.applyPlaceVisit(
            place = "challenge",
            html = "\"Eleven inches!\" he exclaims.",
            character = character(),
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventory,
            sessionLogger = null,
        )

        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.MUSCLE))
        assertNull(inventory.state.value.items[QuestItemRules.BIG_KNOB_SAUSAGE_ID])
    }

    @Test
    fun applyPlaceVisit_challengeSandwich_finishesMystAndConsumes() {
        val prefs = prefs()
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.MYST, QuestDatabase.STARTED)
        val inventory = inventoryWithItem(QuestItemRules.EXORCISED_SANDWICH_ID, "exorcised sandwich")

        GuildQuestSync.applyPlaceVisit(
            place = "challenge",
            html = "You show him the captured poltersandwich.",
            character = character(),
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventory,
            sessionLogger = null,
        )

        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.MYST))
        assertNull(inventory.state.value.items[QuestItemRules.EXORCISED_SANDWICH_ID])
    }

    @Test
    fun applyPlaceVisit_ocgBookTurnIn_finishesEgoAndConsumesBookAndKey() {
        val prefs = prefs()
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.EGO, "step6")
        val inventory = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            init {
                _state.value = InventoryState(
                    items = mapOf(
                        QuestLogSync.DUSTY_BOOK_ID to InventoryItem(
                            QuestLogSync.DUSTY_BOOK_ID,
                            "dusty old book",
                            1,
                            ItemType.OTHER,
                        ),
                        QuestLogSync.FERNSWARTHY_KEY_ID to InventoryItem(
                            QuestLogSync.FERNSWARTHY_KEY_ID,
                            "Fernswarthy's key",
                            1,
                            ItemType.OTHER,
                        ),
                    ),
                )
            }
        }

        GuildQuestSync.applyPlaceVisit(
            place = "ocg",
            html = "Here is your Manual of Labor.",
            character = character(),
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventory,
            sessionLogger = null,
        )

        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.EGO))
        assertNull(inventory.state.value.items[QuestLogSync.DUSTY_BOOK_ID])
        assertNull(inventory.state.value.items[QuestLogSync.FERNSWARTHY_KEY_ID])
    }

    @Test
    fun applyPlaceVisit_ocgBookTurnIn_withoutBook_doesNotFinish() {
        val prefs = prefs()
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.EGO, "step6")
        val inventory = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {}

        GuildQuestSync.applyPlaceVisit(
            place = "ocg",
            html = "Here is your Manual of Transmission.",
            character = character(),
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventory,
            sessionLogger = null,
        )

        assertEquals("step6", db.getProgress(Quest.EGO))
    }

    @Test
    fun applyPlaceVisit_challengeWithoutItem_doesNotFinishQuest() {
        val prefs = prefs()
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.MUSCLE, QuestDatabase.STARTED)
        val inventory = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {}

        GuildQuestSync.applyPlaceVisit(
            place = "challenge",
            html = "\"Eleven inches!\" he exclaims.",
            character = character(),
            preferences = prefs,
            questDatabase = db,
            inventoryManager = inventory,
            sessionLogger = null,
        )

        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.MUSCLE))
    }

    @Test
    fun placeFromUrl_extractsGuildPlace() {
        assertEquals("paco", GuildQuestSync.placeFromUrl("guild.php?place=paco"))
        assertNull(GuildQuestSync.placeFromUrl("guild.php?action=buyskill&skillid=4"))
    }
}
