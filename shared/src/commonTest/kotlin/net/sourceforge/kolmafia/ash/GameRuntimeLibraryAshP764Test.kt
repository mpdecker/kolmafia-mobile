package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import net.sourceforge.kolmafia.session.PvpManager

class GameRuntimeLibraryAshP764Test {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
    }

    private fun trackingFamiliar(): Pair<FamiliarRequest, () -> Int> {
        var stealCalls = 0
        val req = object : FamiliarRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun stealItem(itemId: Int): Result<String> {
                stealCalls++
                return Result.success("ok")
            }
        }
        return req to { stealCalls }
    }

    private fun firecrackerDb(): GameDatabase = object : GameDatabase() {
        override fun item(name: String) = ItemData(
            id = 88,
            name = "knob goblin firecracker",
            descId = "",
            image = "",
            primaryUse = ItemPrimaryUse.NONE,
            secondaryUses = emptySet(),
            access = setOf('t'),
            autosellPrice = 0,
            plural = null,
        )
    }

    @Test
    fun revision_phase764() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shouldFallbackToFamiliarSteal_documentedDualRoute() {
        assertTrue(shouldFallbackToFamiliarSteal("2 knob goblin firecracker"))
        assertFalse(shouldFallbackToFamiliarSteal("flowers Beary Famous"))
        assertFalse(shouldFallbackToFamiliarSteal("5 flowers Beary Famous"))
    }

    @Test
    fun cliSteal_fallbackPrintsDualRouteNotice() {
        val (fam, stealCalls) = trackingFamiliar()
        val lib = GameRuntimeLibrary(familiarRequest = fam, gameDatabase = firecrackerDb())
        val out = outputLib(lib, """cli_execute("steal 2 knob goblin firecracker");""")
        assertTrue(out.contains("Routing to familiar steal"))
        assertEquals(2, stealCalls())
    }

    @Test
    fun famsteal_aliasUsesFamiliarPath() {
        val (fam, stealCalls) = trackingFamiliar()
        val lib = GameRuntimeLibrary(familiarRequest = fam, gameDatabase = firecrackerDb())
        runLib(lib, """cli_execute("famsteal 3 knob goblin firecracker");""")
        assertEquals(3, stealCalls())
    }

    @Test
    fun familiarsteal_aliasUsesFamiliarPath() {
        val (fam, stealCalls) = trackingFamiliar()
        val lib = GameRuntimeLibrary(familiarRequest = fam, gameDatabase = firecrackerDb())
        runLib(lib, """cli_execute("familiarsteal 1 knob goblin firecracker");""")
        assertEquals(1, stealCalls())
    }

    @Test
    fun famsteal_blankPrintsUsage() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("famsteal");""")
        assertTrue(out.contains("Usage: famsteal N item"))
    }
}
