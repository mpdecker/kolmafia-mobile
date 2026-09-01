package net.sourceforge.kolmafia.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings

class TCRSRemoteFetchTest {

    @AfterTest
    fun tearDown() {
        TCRSRemoteFetch.resetSessionCacheForTest()
        TCRSDatabase.reset()
    }

    @Test
    fun fetchText_loadsRepositoryDump() = runTest {
        val fixture = "471\tbouncing spicy batwing\t1\tgood\tMeat Drop: +5"
        val client = HttpClient(MockEngine {
            respond(fixture, HttpStatusCode.OK)
        })
        val result = TCRSRemoteFetch.fetchText(client, "TCRS_Seal_Clubber_Mongoose.txt")
        assertIs<TCRSRemoteFetch.FetchResult.Success>(result)
        assertEquals(fixture, result.text)
    }

    @Test
    fun importFetchedText_persistsThroughPreferences() = runTest {
        val prefs = Preferences(MapSettings())
        val fixture = "471\tbouncing spicy batwing\t1\tgood\tMeat Drop: +5"
        val count = TCRSDatabase.importFetchedText("Seal Clubber", "Mongoose", "", fixture)
        assertEquals(1, count)
        assertTrue(TCRSDatabase.saveToPreferences("Seal Clubber", "Mongoose", prefs))
        TCRSDatabase.reset()
        assertTrue(TCRSDatabase.loadFromPreferences("Seal Clubber", "Mongoose", prefs))
        assertEquals("bouncing spicy batwing", TCRSDatabase.getTCRSName(471))
    }
}
