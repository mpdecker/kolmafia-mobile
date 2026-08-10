package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class AbsorbRequestTest {

    @Test
    fun absorb_rejectsOutsideNoobcore() = runTest {
        val char = KoLCharacter()
        val request = AbsorbRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), char)
        val result = request.absorb("88001")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Gelatinous Noob") == true)
    }

    @Test
    fun absorb_postsInventoryAbsorbUrl() = runTest {
        val captured = mutableListOf<String>()
        val char = noobCharacter(absorbs = 0)
        val engine = MockEngine { request ->
            captured += request.url.toString()
            if (request.url.toString().contains("charpane.php")) {
                respond(
                    """<span><b>Absorptions:</b> 1 / 12</span>""",
                    HttpStatusCode.OK,
                )
            } else {
                respond("ok", HttpStatusCode.OK)
            }
        }
        val db = object : GameDatabase() {
            override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
        }
        ItemDatabase.registerForTest(
            ItemData(
                id = 88001,
                name = "absorb potion",
                descId = "d88001",
                image = "potion.gif",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        val request = AbsorbRequest(HttpClient(engine), char, db)
        val result = request.absorb("absorb potion")
        assertTrue(result.isSuccess)
        assertTrue(captured.any { it.contains("absorb=88001") && it.contains("ajax=1") })
        assertEquals(1, char.state.value.absorbs)
        ItemDatabase.resetForTest()
    }

    @Test
    fun refreshAbsorbs_parsesCharpaneCount() = runTest {
        val char = noobCharacter(absorbs = 0)
        val engine = MockEngine {
            respond(
                """<span><b>Absorptions:</b> 3 / 12</span>""",
                HttpStatusCode.OK,
            )
        }
        val request = AbsorbRequest(HttpClient(engine), char)
        val result = request.refreshAbsorbs()
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
        assertEquals(3, char.state.value.absorbs)
    }

    private fun noobCharacter(absorbs: Int): KoLCharacter =
        KoLCharacter().also { char ->
            char.updateFromApiResponse(
                CharacterApiResponse(
                    path = "Gelatinous Noob",
                    level = "10",
                ),
            )
            char.updateClassResource(absorbs = absorbs)
        }
}
