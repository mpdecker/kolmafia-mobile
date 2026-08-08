package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.FloundryAvailability
import net.sourceforge.kolmafia.data.FloundryDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest

class FloundryRequestTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        FloundryAvailability.resetForTest()
        StandardRequest.resetForTest()
    }

    @Test
    fun purchase_success_setsFloundryItemCreatedPref() = runTest {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "carpe",
                resultQuantity = 1,
                methods = setOf("FLOUNDRY"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true)
        FloundryAvailability.addForTest("carpe", 100)
        ConcoctionDatabase.refreshConcoctionsNow(
            net.sourceforge.kolmafia.data.ConcoctionRefreshContext(
                characterState = CharacterState(),
                preferences = prefs,
            ),
        )
        StandardRequest.parseResponse(
            """<b>Clan Items</b><p><span class="i">Clan Floundry,</span><p>""",
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You acquire an item: carpe.")
        })
        val request = FloundryRequest(client)

        val result = request.purchase("carpe", CharacterState(), prefs)

        assertTrue(result.isSuccess)
        assertTrue(bodies.single().contains("preaction=buyfloundryitem"))
        assertTrue(bodies.single().contains("whichitem=9001"))
        assertTrue(prefs.getBoolean(FloundryRequest.FLOUNDRY_ITEM_CREATED_PREF, false))
    }

    @Test
    fun purchase_noFloundry_failsBeforeHttp() = runTest {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "carpe",
                resultQuantity = 1,
                methods = setOf("FLOUNDRY"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        val client = HttpClient(MockEngine { respond("ok") })
        val request = FloundryRequest(client)

        val result = request.purchase("carpe", CharacterState(), prefs)

        assertTrue(result.isFailure)
    }

    @Test
    fun database_mapsAllSixFloundryItems() {
        assertEquals(6, FloundryDatabase.allItems().size)
        assertEquals(9001, FloundryDatabase.itemIdForName("carpe"))
        assertTrue(FloundryDatabase.isFloundryItem("tunac"))
    }
}
