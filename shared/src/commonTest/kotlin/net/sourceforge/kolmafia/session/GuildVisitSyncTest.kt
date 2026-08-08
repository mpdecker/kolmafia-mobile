package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.GuildCreationSync

class GuildVisitSyncTest {

    private fun prefs() = Preferences(MapSettings())

    private fun character(ascension: Int = 5, characterClass: Int = CharacterClass.PASTAMANCER.id): KoLCharacter =
        KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    classId = characterClass.toString(),
                    ascensions = ascension.toString(),
                ),
            )
        }

    @Test
    fun syncStoreOpen_shopLink_setsLastGuildStoreOpen() {
        val prefs = prefs()
        GuildVisitSync.syncStoreOpen(
            html = """<a href="shop.php">Visit store</a>""",
            character = character(ascension = 7),
            prefs = prefs,
        )
        assertEquals(7, prefs.getInt("lastGuildStoreOpen", -1))
    }

    @Test
    fun syncStoreOpen_noShopLink_leavesPrefUnchanged() {
        val prefs = prefs()
        prefs.setInt("lastGuildStoreOpen", 3)
        GuildVisitSync.syncStoreOpen(
            html = "<html>Guild hall</html>",
            character = character(ascension = 7),
            prefs = prefs,
        )
        assertEquals(3, prefs.getInt("lastGuildStoreOpen", -1))
    }

    @Test
    fun syncStoreOpen_nonStandardClass_skipsPrefWrite() {
        val prefs = prefs()
        GuildVisitSync.syncStoreOpen(
            html = """<a href="shop.php">Visit store</a>""",
            character = character(ascension = 7, characterClass = 999),
            prefs = prefs,
        )
        assertEquals(-1, prefs.getInt("lastGuildStoreOpen", -1))
    }

    @Test
    fun malusByIngredientItemId_resolvesInjectedRecipe() {
        registerPowder()
        injectMalusConcoction()
        assertNotNull(ConcoctionDatabase.malusByIngredientItemId(POWDER_ID))
        assertNotNull(ConcoctionDatabase.chefStaffByBaseItemId(BASE_STICK_ID).let {
            registerStaffItems()
            injectStaffConcoction()
            ConcoctionDatabase.chefStaffByBaseItemId(BASE_STICK_ID)
        })
    }

    @Test
    fun parseFromVisit_malusSuccess_emitsFivePowderConsumed() = runTest(UnconfinedTestDispatcher()) {
        registerPowder()
        injectMalusConcoction()
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        backgroundScope.launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        GuildVisitSync.parseFromVisit(
            url = "guild.php?action=malussmash&whichitem=$POWDER_ID&quantity=1",
            html = "You acquire an item: <b>twinkly nuggets</b>",
            eventBus = bus,
        )

        assertEquals(1, consumed.size)
        assertEquals(POWDER_ID, consumed.single().itemId)
        assertEquals(5, consumed.single().quantity)
    }

    @Test
    fun parseFromVisit_malusFailure_emitsNoEvents() = runTest {
        registerPowder()
        injectMalusConcoction()
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        val job = launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        GuildVisitSync.parseFromVisit(
            url = "guild.php?action=malussmash&whichitem=$POWDER_ID&quantity=1",
            html = "You don't have enough twinkly powder.",
            eventBus = bus,
        )

        job.cancel()
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun parseFromVisit_staffSuccess_emitsAllIngredients() = runTest(UnconfinedTestDispatcher()) {
        registerStaffItems()
        injectStaffConcoction()
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        backgroundScope.launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        GuildVisitSync.parseFromVisit(
            url = "guild.php?action=makestaff&whichstaff=$BASE_STICK_ID",
            html = "You acquire an item: <b>Staff of the Teapot Tempest</b>",
            eventBus = bus,
        )

        assertEquals(6, consumed.size)
        assertTrue(consumed.any { it.itemId == BASE_STICK_ID && it.quantity == 1 })
        assertTrue(consumed.any { it.itemId == MENUDO_ID && it.quantity == 1 })
    }

    @Test
    fun parseFromVisit_staffMissingIngredients_emitsNoEvents() = runTest {
        registerStaffItems()
        injectStaffConcoction()
        val bus = GameEventBus()
        val consumed = mutableListOf<GameEvent.ItemConsumed>()
        val job = launch {
            bus.events.collect { event ->
                if (event is GameEvent.ItemConsumed) consumed += event
            }
        }

        GuildCreationSync.parseStaff(
            url = "guild.php?action=makestaff&whichstaff=$BASE_STICK_ID",
            responseText = "You don't have all of the items I'll need to make that Chefstaff.",
            eventBus = bus,
        )

        job.cancel()
        assertTrue(consumed.isEmpty())
    }

    private fun registerPowder() {
        ItemDatabase.registerForTest(
            ItemData(
                id = POWDER_ID,
                name = "twinkly powder",
                descId = "d$POWDER_ID",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
    }

    private fun injectMalusConcoction() {
        ConcoctionDatabase.resetForTest()
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "twinkly nuggets",
                resultQuantity = 1,
                methods = setOf("MALUS"),
                ingredients = listOf(ConcoctionIngredient("twinkly powder", 5)),
            ),
        )
    }

    private fun registerStaffItems() {
        registerItem(BASE_STICK_ID, "big stirring stick")
        registerItem(MENUDO_ID, "menudo")
        registerItem(SANGRIA_ID, "sangria")
        registerItem(TEA_ID, "hippy herbal tea")
        registerItem(PILL_ID, "concentrated magicalness pill")
        registerItem(JUICE_ID, "magical mystery juice (3)")
    }

    private fun injectStaffConcoction() {
        ConcoctionDatabase.resetForTest()
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "Staff of the Teapot Tempest",
                resultQuantity = 1,
                methods = setOf("STAFF"),
                ingredients = listOf(
                    ConcoctionIngredient("big stirring stick", 1),
                    ConcoctionIngredient("menudo", 1),
                    ConcoctionIngredient("sangria", 1),
                    ConcoctionIngredient("hippy herbal tea", 1),
                    ConcoctionIngredient("concentrated magicalness pill", 1),
                    ConcoctionIngredient("magical mystery juice (3)", 1),
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
        private const val POWDER_ID = 88401
        private const val BASE_STICK_ID = 88201
        private const val MENUDO_ID = 88202
        private const val SANGRIA_ID = 88203
        private const val TEA_ID = 88204
        private const val PILL_ID = 88205
        private const val JUICE_ID = 88206
    }
}
