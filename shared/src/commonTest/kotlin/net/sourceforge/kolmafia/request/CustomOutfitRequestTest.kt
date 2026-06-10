package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomOutfitRequestTest {

    @Test
    fun parseOutfitPieces_readsCommentBlocks() {
        val html = """
            <!-- outfitid: -3 -->
            <ul><li>miner's helmet</li><li>7-Foot Dwarven mattock</li></ul>
            <!-- outfitid: -5 -->
            <ul><li>lucky pendant</li></ul>
        """.trimIndent()
        val pieces = CustomOutfitRequest(HttpClient(MockEngine { respond("") })).parseOutfitPieces(html)
        assertEquals(listOf("miner's helmet", "7-Foot Dwarven mattock"), pieces[-3])
        assertEquals(listOf("lucky pendant"), pieces[-5])
    }

    @Test
    fun fetchCustomOutfits_mergesOptionsWithPieces() = kotlinx.coroutines.test.runTest {
        val html = """
            <!-- outfitid: -3 -->
            <li>miner's helmet</li>
        """.trimIndent()
        val client = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })
        val outfits = CustomOutfitRequest(client).fetchCustomOutfits(listOf(-3 to "Mining Custom"))
        assertEquals(1, outfits.size)
        assertEquals(-3, outfits[0].id)
        assertEquals(listOf("miner's helmet"), outfits[0].equipment)
    }
}
