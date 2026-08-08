package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

@OptIn(ExperimentalCoroutinesApi::class)
class GameRuntimeLibraryAshP321Test {

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun visitHook_sushiPhp_deductsIngredientsAndBumpsFullness() = runTest(UnconfinedTestDispatcher()) {
        registerNigiri()
        val character = KoLCharacter().also {
            it.updateConsumables(fullness = 5, inebriety = 0, spleenUsed = 0)
        }
        val eventBus = GameEventBus()
        val events = mutableListOf<GameEvent>()
        val job = launch { eventBus.events.collect { events += it } }
        val lib = GameRuntimeLibrary(
            character = character,
            eventBus = eventBus,
            preferences = Preferences(MapSettings()),
        )

        lib.processVisitResponseHooks(
            html = "You eat the beefy nigiri. Delicious!",
            url = "https://www.kingdomofloathing.com/sushi.php?action=Yep.&whichsushi=1",
        )

        job.cancel()
        assertEquals(6, character.state.value.fullness)
        assertEquals(
            listOf(
                GameEvent.ItemConsumed(BEEFY_FISH_ID, 1),
                GameEvent.ItemConsumed(WHITE_RICE_ID, 1),
            ),
            events.filterIsInstance<GameEvent.ItemConsumed>(),
        )
    }

    @Test
    fun visitHook_sushiPhpWithoutWhichsushi_isNoOp() = runTest(UnconfinedTestDispatcher()) {
        registerNigiri()
        val character = KoLCharacter().also {
            it.updateConsumables(fullness = 5, inebriety = 0, spleenUsed = 0)
        }
        val eventBus = GameEventBus()
        val events = mutableListOf<GameEvent>()
        val job = launch { eventBus.events.collect { events += it } }
        val lib = GameRuntimeLibrary(
            character = character,
            eventBus = eventBus,
            preferences = Preferences(MapSettings()),
        )

        lib.processVisitResponseHooks(
            html = "You eat the beefy nigiri. Delicious!",
            url = "https://www.kingdomofloathing.com/sushi.php",
        )

        job.cancel()
        assertEquals(5, character.state.value.fullness)
        assertEquals(emptyList(), events.filterIsInstance<GameEvent.ItemConsumed>())
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
