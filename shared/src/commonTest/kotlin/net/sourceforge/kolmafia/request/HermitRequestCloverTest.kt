package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.AscensionPath
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences

class HermitRequestCloverTest {

    @Test
    fun parseCloverCount_extractsStockFromHtml() {
        val html = """
            <p>You have 3 worthless items.</p>
            <p>11-leaf clover: 2 left in stock for today</p>
        """.trimIndent()
        assertEquals(2, HermitRequest(HttpClient(MockEngine { respond("") })).parseCloverCount(html))
    }

    @Test
    fun fetchCloverCount_parsesHermitPage() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                "1 left in stock for today",
                HttpStatusCode.OK,
            )
        })
        assertEquals(1, HermitRequest(client).fetchCloverCount())
    }

    @Test
    fun fetchCloverCount_zombiePathBeforeUse() = runTest {
        val prefs = Preferences(MapSettings())
        assertEquals(
            1,
            HermitRequest(HttpClient(MockEngine { respond("") }))
                .fetchCloverCount(AscensionPath.ZOMBIE_SLAYER, prefs),
        )
    }

    @Test
    fun fetchCloverCount_zombiePathAfterUse() = runTest {
        val prefs = Preferences(MapSettings()).also { it.setBoolean("_zombieClover0", true) }
        assertEquals(
            0,
            HermitRequest(HttpClient(MockEngine { respond("") }))
                .fetchCloverCount(AscensionPath.ZOMBIE_SLAYER, prefs),
        )
    }
}
