package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.mall.MallListing
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest

class GameRuntimeLibraryAshP524Test {

    private class FakeMallManager(
        private val listings: List<MallListing>,
    ) : MallManager(
        MallSearchRequest(HttpClient(MockEngine { respond("") })),
        MallPurchaseRequest(HttpClient(MockEngine { respond("") })),
        null,
    ) {
        var lastLimit: Int = -1
        override suspend fun searchListings(itemName: String, limit: Int): List<MallListing> {
            lastLimit = limit
            return listings.take(if (limit <= 0) listings.size else limit)
        }
    }

    @Test
    fun revision_phase524() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun searchmall_printsLiveRows() {
        val mall = FakeMallManager(
            listOf(
                MallListing(1, "Shop A", 2, 100, 5),
                MallListing(2, "Shop B", 2, 250, 3),
            ),
        )
        val out = outputLib(
            GameRuntimeLibrary(mallManager = mall),
            """cli_execute("searchmall seal tooth");""",
        )
        assertTrue(out.contains("100"))
        assertTrue(out.contains("250"))
        assertTrue(out.contains("5") || out.contains("@ 100"))
        assertTrue(out.contains("3") || out.contains("@ 250"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun searchmall_withLimit_honorsLimit() {
        val mall = FakeMallManager(
            listOf(
                MallListing(1, "", 2, 100, 5),
                MallListing(2, "", 2, 250, 3),
            ),
        )
        val out = outputLib(
            GameRuntimeLibrary(mallManager = mall),
            """cli_execute("searchmall seal tooth with limit 1");""",
        )
        assertEquals(1, mall.lastLimit)
        assertTrue(out.contains("100"))
        assertFalse(out.contains("250"))
    }

    @Test
    fun searchmall_empty_isSilent() {
        val mall = FakeMallManager(emptyList())
        val out = outputLib(
            GameRuntimeLibrary(mallManager = mall),
            """cli_execute("searchmall zzznosuchitem999");""",
        )
        assertEquals("", out.trim())
    }
}
