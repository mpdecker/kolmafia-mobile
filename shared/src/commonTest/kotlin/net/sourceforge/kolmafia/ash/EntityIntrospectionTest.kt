package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.DescriptionCache
import net.sourceforge.kolmafia.data.GameDatabase

class EntityIntrospectionTest {

    @AfterTest
    fun cleanup() {
        DescriptionCache.clear()
    }

    @Test
    fun entityDesc_prefetchesItemOnCacheMiss() = runBlocking {
        DescriptionCache.clear()
        val db = GameDatabase()
        db.load()

        val engine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("desc_item.php"))
            respond(
                """<div id="description"><p>Fresh item text.</p><script>""",
                HttpStatusCode.OK,
            )
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            httpClient = HttpClient(engine),
        )

        assertEquals(
            "<p>Fresh item text.</p>",
            lib.entityDesc(AshType.ITEM, "seal tooth"),
        )
    }

    @Test
    fun entityDesc_skipsFetchWithoutHttpClient() = runBlocking {
        DescriptionCache.clear()
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db, httpClient = null)

        assertEquals("", lib.entityDesc(AshType.ITEM, "seal tooth"))
    }
}
