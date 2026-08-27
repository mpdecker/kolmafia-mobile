package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class ResultProcessorTest {
    private lateinit var prefs: Preferences
    private lateinit var quests: QuestDatabase

    @BeforeTest
    fun setUp() {
        ResultProcessor.resetForTest()
        prefs = Preferences(MapSettings())
        quests = QuestDatabase(prefs)
        ResultProcessor.questDatabaseProvider = { quests }
        ResultProcessor.ascensionNumberProvider = { 3 }
    }

    @Test
    fun parseItemsAndMeat() {
        val html = """
            You acquire an item: <b>seal tooth</b>
            You acquire an item: <b>seal tooth</b>
            You gain 150 Meat
        """.trimIndent()
        val parsed = ResultProcessor.parseResults(html)
        assertEquals(listOf("seal tooth" to 2), parsed.items)
        assertEquals(150, parsed.meat)
    }

    @Test
    fun parseEffects() {
        val html = """
            You acquire an effect: <b>Hot Breath</b><br>(duration: 5 Adventures)
            You lose an effect: <b>Disco Leer</b>
        """.trimIndent()
        val parsed = ResultProcessor.parseResults(html)
        assertEquals(listOf("Hot Breath" to 5), parsed.effectsGained)
        assertEquals(listOf("Disco Leer"), parsed.effectsLost)
    }

    @Test
    fun processResultsGainsInventory() = runBlocking {
        ItemDatabase.load()
        val item = ItemDatabase.getByName("seal tooth")
        requireNotNull(item)
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val client = HttpClient(engine)
        val inv = InventoryManager(client, GameEventBus())
        ResultProcessor.processResults(
            adventureResults = true,
            html = "You acquire an item: <b>seal tooth</b>",
            inventory = inv,
            character = KoLCharacter(),
            preferences = prefs,
            questDatabase = quests,
        )
        assertEquals(1, inv.state.value.items[item.id]?.quantity)
    }

    @Test
    fun gainItemOysterEggPref() {
        ResultProcessor.gainItem(
            adventureResults = true,
            itemId = ItemPool.MAGNIFICENT_OYSTER_EGG,
            count = 2,
            preferences = prefs,
            questDatabase = quests,
        )
        assertEquals(2, prefs.getInt("_oysterEggsFound", 0))
    }

    @Test
    fun gainItemMacguffinDiary() {
        ResultProcessor.gainItem(
            adventureResults = true,
            itemId = ItemPool.MACGUFFIN_DIARY,
            count = 1,
            preferences = prefs,
            questDatabase = quests,
        )
        assertEquals("step3", quests.getProgress(Quest.BLACK))
    }

    @Test
    fun familiarPoundFlag() {
        val html = "Your familiar gains a pound"
        assertTrue(ResultProcessor.parseResults(html).familiarGainedPound)
    }

    @Test
    fun processEffectsViaManager() {
        val engine = MockEngine { respond("{}", HttpStatusCode.OK) }
        val client = HttpClient(engine)
        val effects = EffectManager(client, GameEventBus())
        ResultProcessor.processResults(
            adventureResults = true,
            html = "You acquire an effect: <b>Hot Breath</b><br>(duration: 5 Adventures)",
            effectManager = effects,
            preferences = prefs,
            questDatabase = quests,
        )
        assertEquals("Hot Breath", effects.state.value.effects.single().name)
        assertEquals(5, effects.state.value.effects.single().duration)
    }

    @Test
    fun gainItemLarvaAndPirateAndIsland() {
        ResultProcessor.gainItem(true, ItemPool.MOSQUITO_LARVA, 1, preferences = prefs, questDatabase = quests)
        assertEquals("step1", quests.getProgress(Quest.LARVA))

        ResultProcessor.gainItem(true, ItemPool.PIRATE_FLEDGES, 1, preferences = prefs, questDatabase = quests)
        assertEquals("step6", quests.getProgress(Quest.PIRATE))

        ResultProcessor.gainItem(true, ItemPool.DINGY_DINGHY, 1, preferences = prefs, questDatabase = quests)
        assertEquals(3, prefs.getInt("lastIslandUnlock", -1))
    }

    @Test
    fun gainItemSockConsumesImmateria() = runBlocking {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val inv = InventoryManager(HttpClient(engine), GameEventBus())
        inv.gainItemLocally(ItemPool.TISSUE_PAPER_IMMATERIA, 1)
        inv.gainItemLocally(ItemPool.TIN_FOIL_IMMATERIA, 1)
        inv.gainItemLocally(ItemPool.GAUZE_IMMATERIA, 1)
        inv.gainItemLocally(ItemPool.PLASTIC_WRAP_IMMATERIA, 1)
        ResultProcessor.inventoryProvider = { inv }
        ResultProcessor.gainItem(true, ItemPool.SOCK, 1, preferences = prefs, questDatabase = quests, inventory = inv)
        assertEquals("step7", quests.getProgress(Quest.GARBAGE))
        assertEquals(null, inv.state.value.items[ItemPool.TISSUE_PAPER_IMMATERIA])
    }

    @Test
    fun gainItemPalindomeMegaGem() {
        quests.setProgress(Quest.PALINDOME, "step3")
        ResultProcessor.gainItem(true, ItemPool.WET_STUNT_NUT_STEW, 1, preferences = prefs, questDatabase = quests)
        assertEquals("step4", quests.getProgress(Quest.PALINDOME))
        ResultProcessor.gainItem(true, ItemPool.MEGA_GEM, 1, preferences = prefs, questDatabase = quests)
        assertEquals("step5", quests.getProgress(Quest.PALINDOME))
    }

    @Test
    fun gainItemIotmDropCounters() {
        ResultProcessor.gainItem(true, ItemPool.AGUA_DE_VIDA, 1, preferences = prefs, questDatabase = quests)
        assertEquals(1, prefs.getInt("_aguaDrops", 0))
        ResultProcessor.gainItem(true, ItemPool.DEVILISH_FOLIO, 1, preferences = prefs, questDatabase = quests)
        assertEquals(1, prefs.getInt("_kloopDrops", 0))
        ResultProcessor.gainItem(true, ItemPool.GROOSE_GREASE, 1, preferences = prefs, questDatabase = quests)
        assertEquals(1, prefs.getInt("_grooseDrops", 0))
        ResultProcessor.gainItem(true, ItemPool.GG_TOKEN, 1, preferences = prefs, questDatabase = quests)
        assertEquals(3, prefs.getInt("lastArcadeAscension", -1))
    }

    @Test
    fun gainItemSwaggerAvailabilityAndBat() {
        ResultProcessor.gainItem(true, ItemPool.BLACK_BARTS_BOOTY, 1, preferences = prefs, questDatabase = quests)
        assertEquals(false, prefs.getBoolean("blackBartsBootyAvailable", true))
        ResultProcessor.gainItem(true, ItemPool.SHADOW_SAUSAGE, 1, preferences = prefs, questDatabase = quests)
        assertTrue(prefs.getBoolean("_rufusShadowItemSeen", false))
        assertTrue(BatManager.gainItem(ItemPool.EXPERIMENTAL_GENE_THERAPY, prefs))
    }

    @Test
    fun autoCreateBlackbird() = runBlocking {
        prefs.setBoolean("autoCraft", true)
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val inv = InventoryManager(HttpClient(engine), GameEventBus())
        inv.gainItemLocally(ItemPool.BROKEN_WINGS, 1)
        inv.gainItemLocally(ItemPool.SUNKEN_EYES, 1)
        ResultProcessor.inventoryProvider = { inv }
        ResultProcessor.gainItem(
            true,
            ItemPool.BROKEN_WINGS,
            1,
            preferences = prefs,
            questDatabase = quests,
            inventory = inv,
        )
        // After gain of wings (already had both), autoCreate should craft blackbird
        assertTrue((inv.state.value.items[ItemPool.REASSEMBLED_BLACKBIRD]?.quantity ?: 0) >= 1)
    }
}
