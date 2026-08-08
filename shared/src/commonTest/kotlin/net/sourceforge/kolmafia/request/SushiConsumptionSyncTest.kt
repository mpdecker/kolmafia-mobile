package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConsumableQuality
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

@OptIn(ExperimentalCoroutinesApi::class)
class SushiConsumptionSyncTest {

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun parseConsumption_deductsIngredients() = runTest(UnconfinedTestDispatcher()) {
        registerNigiri()
        val events = mutableListOf<GameEvent>()
        val eventBus = GameEventBus()
        val job = launch { eventBus.events.collect { events += it } }

        SushiConsumptionSync.parseConsumption(
            formFields = SushiChoiceMapper.formFields("beefy nigiri")!!,
            responseText = "You eat the beefy nigiri. Delicious!",
            updateFullness = false,
            eventBus = eventBus,
        )

        job.cancel()
        assertEquals(
            listOf(
                GameEvent.ItemConsumed(BEEFY_FISH_ID, 1),
                GameEvent.ItemConsumed(WHITE_RICE_ID, 1),
            ),
            events.filterIsInstance<GameEvent.ItemConsumed>(),
        )
    }

    @Test
    fun parseConsumption_skipsOnTooFull() = runTest(UnconfinedTestDispatcher()) {
        registerNigiri()
        val events = mutableListOf<GameEvent>()
        val eventBus = GameEventBus()
        val job = launch { eventBus.events.collect { events += it } }

        SushiConsumptionSync.parseConsumption(
            formFields = SushiChoiceMapper.formFields("beefy nigiri")!!,
            responseText = "You are way too full to eat it right now.",
            updateFullness = true,
            eventBus = eventBus,
        )

        job.cancel()
        assertTrue(events.filterIsInstance<GameEvent.ItemConsumed>().isEmpty())
    }

    @Test
    fun parseConsumption_bumpsFullnessWhenBodyLacksFullnessDisplay() = runTest {
        registerNigiri()
        val character = KoLCharacter()
        character.updateConsumables(fullness = 5, inebriety = 0, spleenUsed = 0)

        SushiConsumptionSync.parseConsumption(
            formFields = SushiChoiceMapper.formFields("beefy nigiri")!!,
            responseText = "You eat the beefy nigiri. Delicious!",
            updateFullness = true,
            character = character,
        )

        assertEquals(6, character.state.value.fullness)
    }

    @Test
    fun parseConsumption_doesNotBumpFullnessWhenBodyShowsFullness() = runTest {
        registerNigiri()
        val character = KoLCharacter()
        character.updateConsumables(fullness = 5, inebriety = 0, spleenUsed = 0)

        SushiConsumptionSync.parseConsumption(
            formFields = SushiChoiceMapper.formFields("beefy nigiri")!!,
            responseText = "You eat the beefy nigiri.<br>Fullness +1",
            updateFullness = true,
            character = character,
        )

        assertEquals(5, character.state.value.fullness)
    }

    @Test
    fun parseConsumption_fancyDoilyConsumes6328() = runTest(UnconfinedTestDispatcher()) {
        registerNigiri()
        val events = mutableListOf<GameEvent>()
        val eventBus = GameEventBus()
        val job = launch { eventBus.events.collect { events += it } }

        SushiConsumptionSync.parseConsumption(
            formFields = SushiChoiceMapper.formFields("beefy nigiri")!!,
            responseText = "Eating it off of a fancy doily makes it even more delicious!",
            updateFullness = false,
            eventBus = eventBus,
        )

        job.cancel()
        assertTrue(events.filterIsInstance<GameEvent.ItemConsumed>().any { it.itemId == 6328 })
    }

    @Test
    fun handleWorktea_setsPrefAndEmitsConsumeEvent() = runTest(UnconfinedTestDispatcher()) {
        val prefs = Preferences(MapSettings())
        val events = mutableListOf<GameEvent>()
        val eventBus = GameEventBus()
        val job = launch { eventBus.events.collect { events += it } }

        SushiConsumptionSync.handleWorktea(
            responseText = "the leaves in the bottom look just like <b>an eel</b>!",
            preferences = prefs,
            eventBus = eventBus,
        )

        job.cancel()
        assertEquals("an eel", prefs.getString("workteaClue", ""))
        assertEquals(1, prefs.getInt("dreadScroll7", 0))
        assertEquals(listOf(GameEvent.ItemConsumed(6356, 1)), events.filterIsInstance<GameEvent.ItemConsumed>())
    }

    @Test
    fun registerRequest_appendsSessionLogLine() {
        registerNigiri()
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())

        SushiConsumptionSync.registerRequest(
            formFields = SushiChoiceMapper.formFields("beefy nigiri")!!,
            sessionLogger = sessionLogger,
        )

        val lines = sessionLogger.recentLines()
        assertTrue(lines.any { it.contains("Roll and eat beefy nigiri from 1 beefy fish meat, 1 white rice") })
    }

    @Test
    fun parseConsumptionFromVisit_deductsIngredients() = runTest(UnconfinedTestDispatcher()) {
        registerNigiri()
        val events = mutableListOf<GameEvent>()
        val eventBus = GameEventBus()
        val job = launch { eventBus.events.collect { events += it } }

        SushiConsumptionSync.parseConsumptionFromVisit(
            url = "sushi.php?action=Yep.&whichsushi=1",
            responseText = "You eat the beefy nigiri. Delicious!",
            eventBus = eventBus,
        )

        job.cancel()
        assertEquals(
            listOf(
                GameEvent.ItemConsumed(BEEFY_FISH_ID, 1),
                GameEvent.ItemConsumed(WHITE_RICE_ID, 1),
            ),
            events.filterIsInstance<GameEvent.ItemConsumed>(),
        )
    }

    @Test
    fun parseConsumptionFromVisit_skipsOnTooFull() = runTest(UnconfinedTestDispatcher()) {
        registerNigiri()
        val events = mutableListOf<GameEvent>()
        val eventBus = GameEventBus()
        val job = launch { eventBus.events.collect { events += it } }

        SushiConsumptionSync.parseConsumptionFromVisit(
            url = "sushi.php?whichsushi=1",
            responseText = "You are way too full to eat it right now.",
            eventBus = eventBus,
        )

        job.cancel()
        assertTrue(events.filterIsInstance<GameEvent.ItemConsumed>().isEmpty())
    }

    @Test
    fun parseConsumptionFromVisit_bumpsFullnessWhenBodyLacksFullnessDisplay() = runTest {
        registerNigiri()
        val character = KoLCharacter()
        character.updateConsumables(fullness = 5, inebriety = 0, spleenUsed = 0)

        SushiConsumptionSync.parseConsumptionFromVisit(
            url = "sushi.php?whichsushi=1",
            responseText = "You eat the beefy nigiri. Delicious!",
            character = character,
        )

        assertEquals(6, character.state.value.fullness)
    }

    @Test
    fun parseConsumptionFromVisit_noOpWithoutWhichsushi() = runTest(UnconfinedTestDispatcher()) {
        registerNigiri()
        val events = mutableListOf<GameEvent>()
        val eventBus = GameEventBus()
        val job = launch { eventBus.events.collect { events += it } }

        SushiConsumptionSync.parseConsumptionFromVisit(
            url = "sushi.php",
            responseText = "You eat the beefy nigiri. Delicious!",
            eventBus = eventBus,
        )

        job.cancel()
        assertTrue(events.filterIsInstance<GameEvent.ItemConsumed>().isEmpty())
    }

    private fun registerNigiri() {
        registerItem(BEEFY_FISH_ID, "beefy fish meat")
        registerItem(WHITE_RICE_ID, "white rice")
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "beefy nigiri",
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 4,
                advMax = 8,
                muscMin = 8,
                muscMax = 16,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "beefy nigiri",
                resultQuantity = 1,
                methods = setOf("SUSHI"),
                ingredients = listOf(
                    ConcoctionIngredient("beefy fish meat", 1),
                    ConcoctionIngredient("white rice", 1),
                ),
            ),
        )
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
                autosellPrice = 100,
                plural = null,
            ),
        )
    }

    companion object {
        private const val BEEFY_FISH_ID = 89201
        private const val WHITE_RICE_ID = 89202
    }
}
