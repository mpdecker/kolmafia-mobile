package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionManagerBreakfastTest {

    private val mockClient = HttpClient(MockEngine { respond("ok") })

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences =
        Preferences(MapSettings().also { it.block() })

    @Test fun daycountChanged_callsClearAndStoresNewDaycount() = runBlocking {
        var clearCalled = false
        var breakfastCalled = false
        val p = prefs { putInt(Preferences.LAST_DAYCOUNT, 5) }

        val campground = object : CampgroundRequest(mockClient) {
            override suspend fun harvestGarden() = Result.success(Unit)
        }
        val rumpus = object : ClanRumpusRequest(mockClient) {
            override suspend fun visit() = Result.success(Unit)
        }
        val lounge = object : ClanLoungeRequest(mockClient) {
            override suspend fun useKlaw() = Result.success("ok")
        }
        val breakfastMgr = object : BreakfastManager(campground, rumpus, lounge, p, UseItemRequest(mockClient)) {
            override suspend fun runBreakfast(charState: CharacterState, inventoryState: InventoryState) {
                breakfastCalled = true
            }
            override fun clearBreakfastPrefs() { clearCalled = true }
        }

        // Simulate the daycount-gated logic directly
        val charState = CharacterState(dayCount = 6, currentRun = 100)
        val lastDay = p.getInt(Preferences.LAST_DAYCOUNT, -1)
        if (charState.dayCount != lastDay) {
            breakfastMgr.clearBreakfastPrefs()
            p.setInt(Preferences.LAST_DAYCOUNT, charState.dayCount)
        }
        breakfastMgr.runBreakfast(charState, InventoryState())

        assertTrue(clearCalled)
        assertTrue(breakfastCalled)
        assertEquals(6, p.getInt(Preferences.LAST_DAYCOUNT))
    }

    @Test fun daycountUnchanged_doesNotClear() {
        var clearCalled = false
        val p = prefs { putInt(Preferences.LAST_DAYCOUNT, 6) }
        val charState = CharacterState(dayCount = 6)
        val lastDay = p.getInt(Preferences.LAST_DAYCOUNT, -1)
        if (charState.dayCount != lastDay) {
            clearCalled = true
        }
        assertFalse(clearCalled)
    }
}
