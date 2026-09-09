package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class ResultProcessorAutoCreateXxxivTest {
    private lateinit var prefs: Preferences
    private lateinit var quests: QuestDatabase
    private lateinit var inv: InventoryManager

    @BeforeTest
    fun setUp() = runBlocking {
        ResultProcessor.resetForTest()
        prefs = Preferences(MapSettings())
        prefs.setBoolean("autoCraft", true)
        quests = QuestDatabase(prefs)
        ResultProcessor.questDatabaseProvider = { quests }
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        inv = InventoryManager(HttpClient(engine), GameEventBus())
        ResultProcessor.inventoryProvider = { inv }
    }

    @AfterTest
    fun tearDown() {
        ResultProcessor.resetForTest()
    }

    @Test
    fun autoCreateBadassBeltFromBatskinAndSkull() = runBlocking {
        inv.gainItemLocally(ItemPool.BATSKIN_BELT, 1)
        inv.gainItemLocally(ItemPool.BONERDAGON_SKULL, 1)
        ResultProcessor.gainItem(
            adventureResults = true,
            itemId = ItemPool.BONERDAGON_SKULL,
            count = 1,
            preferences = prefs,
            questDatabase = quests,
            inventory = inv,
        )
        assertTrue((inv.state.value.items[ItemPool.BADASS_BELT]?.quantity ?: 0) >= 1)
        assertEquals(0, inv.state.value.items[ItemPool.BATSKIN_BELT]?.quantity ?: 0)
        assertEquals(0, inv.state.value.items[ItemPool.BONERDAGON_SKULL]?.quantity ?: 0)
    }

    @Test
    fun autoCreateBadassBeltRequiresAdventureResults() = runBlocking {
        inv.gainItemLocally(ItemPool.BATSKIN_BELT, 1)
        inv.gainItemLocally(ItemPool.BONERDAGON_SKULL, 1)
        ResultProcessor.gainItem(
            adventureResults = false,
            itemId = ItemPool.BONERDAGON_SKULL,
            count = 1,
            preferences = prefs,
            questDatabase = quests,
            inventory = inv,
        )
        assertEquals(0, inv.state.value.items[ItemPool.BADASS_BELT]?.quantity ?: 0)
        assertEquals(1, inv.state.value.items[ItemPool.BATSKIN_BELT]?.quantity ?: 0)
        assertEquals(1, inv.state.value.items[ItemPool.BONERDAGON_SKULL]?.quantity ?: 0)
    }

    @Test
    fun autoCreateBonerdagonNecklaceFromHempAndVertebra() = runBlocking {
        inv.gainItemLocally(ItemPool.HEMP_STRING, 1)
        inv.gainItemLocally(ItemPool.BONERDAGON_VERTEBRA, 1)
        ResultProcessor.gainItem(
            adventureResults = true,
            itemId = ItemPool.BONERDAGON_VERTEBRA,
            count = 1,
            preferences = prefs,
            questDatabase = quests,
            inventory = inv,
        )
        assertTrue((inv.state.value.items[ItemPool.BONERDAGON_NECKLACE]?.quantity ?: 0) >= 1)
        assertEquals(0, inv.state.value.items[ItemPool.HEMP_STRING]?.quantity ?: 0)
        assertEquals(0, inv.state.value.items[ItemPool.BONERDAGON_VERTEBRA]?.quantity ?: 0)
    }

    @Test
    fun autoCreateTalismanFromBothCharms() = runBlocking {
        inv.gainItemLocally(ItemPool.COPPERHEAD_CHARM, 1)
        inv.gainItemLocally(ItemPool.COPPERHEAD_CHARM_RAMPANT, 1)
        ResultProcessor.gainItem(
            adventureResults = true,
            itemId = ItemPool.COPPERHEAD_CHARM_RAMPANT,
            count = 1,
            preferences = prefs,
            questDatabase = quests,
            inventory = inv,
        )
        assertTrue((inv.state.value.items[ItemPool.TALISMAN]?.quantity ?: 0) >= 1)
        assertEquals(0, inv.state.value.items[ItemPool.COPPERHEAD_CHARM]?.quantity ?: 0)
        assertEquals(0, inv.state.value.items[ItemPool.COPPERHEAD_CHARM_RAMPANT]?.quantity ?: 0)
        assertEquals(QuestDatabase.FINISHED, quests.getProgress(Quest.SHEN))
        assertEquals(QuestDatabase.FINISHED, quests.getProgress(Quest.RON))
    }

    @Test
    fun autoCreateTalismanStartsPalindomeQuest() = runBlocking {
        ResultProcessor.gainItem(
            adventureResults = true,
            itemId = ItemPool.TALISMAN,
            count = 1,
            preferences = prefs,
            questDatabase = quests,
            inventory = inv,
        )
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.PALINDOME))
    }

    @Test
    fun autoCreateSkipsWhenAutoCraftDisabled() = runBlocking {
        prefs.setBoolean("autoCraft", false)
        inv.gainItemLocally(ItemPool.HEMP_STRING, 1)
        inv.gainItemLocally(ItemPool.BONERDAGON_VERTEBRA, 1)
        ResultProcessor.gainItem(
            adventureResults = true,
            itemId = ItemPool.BONERDAGON_VERTEBRA,
            count = 1,
            preferences = prefs,
            questDatabase = quests,
            inventory = inv,
        )
        assertEquals(0, inv.state.value.items[ItemPool.BONERDAGON_NECKLACE]?.quantity ?: 0)
        assertEquals(1, inv.state.value.items[ItemPool.HEMP_STRING]?.quantity ?: 0)
        assertEquals(1, inv.state.value.items[ItemPool.BONERDAGON_VERTEBRA]?.quantity ?: 0)
    }

    @Test
    fun citadelSatchelDeductsMeat() = runBlocking {
        val character = KoLCharacter().also { it.updateMeat(500) }
        ResultProcessor.characterProvider = { character }
        ResultProcessor.gainItem(
            adventureResults = true,
            itemId = ItemPool.CITADEL_SATCHEL,
            count = 1,
            preferences = prefs,
            questDatabase = quests,
            inventory = inv,
        )
        assertEquals(200, character.state.value.meat)
    }
}
