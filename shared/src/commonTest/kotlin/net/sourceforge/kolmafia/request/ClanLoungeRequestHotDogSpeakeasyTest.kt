package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionRefreshContext
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.preferences.Preferences

class ClanLoungeRequestHotDogSpeakeasyTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        HotDogAvailability.resetForTest()
    }

    @Test
    fun visitHotDogStand_withPreferences_syncsAvailabilityAndRuntime() = runTest {
        val html = """
            <table><tr><form action=clan_viplounge.php method=post>
            <tr><td><input class=button type=submit value=Eat><span><b>basic hot dog</b></span></td></tr>
            </form></table>
        """.trimIndent()
        val prefs = Preferences(MapSettings())
        val client = HttpClient(MockEngine { respond(html) })

        val result = ClanLoungeRequest(client).visitHotDogStand(prefs)

        assertTrue(result.isSuccess)
        assertTrue(HotDogAvailability.isAvailable("basic hot dog"))
        assertEquals(1, ConcoctionDatabase.totalCount("basic hot dog"))
    }

    @Test
    fun eatHotDogFancy_withPreferences_zeroesRuntimeAfterSuccess() = runTest {
        HotDogAvailability.addForTest("sly dog")
        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.EMPTY)
        assertEquals(1, ConcoctionDatabase.totalCount("sly dog"))

        val prefs = Preferences(MapSettings())
        val client = HttpClient(MockEngine { respond("You gain some stats.") })

        val result = ClanLoungeRequest(client).eatHotDog(-95, prefs)

        assertTrue(result.isSuccess)
        assertTrue(prefs.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false))
        assertEquals(0, ConcoctionDatabase.totalCount("sly dog"))
    }

    @Test
    fun preflightHotDog_blocksWhenNotPermitted() {
        val state = CharacterState(limitMode = "ed")
        HotDogAvailability.addForTest("basic hot dog")
        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.EMPTY)

        val result = ClanLoungeRequest.preflightHotDog("basic hot dog", state, null)

        assertTrue(result.isFailure)
    }
}
