package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap

class GameRuntimeLibraryAshP488Test {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
        EffectDatabase.resetForTest()
        UneffectSkillEffectMap.resetForTest()
    }

    private fun lines(out: String): List<String> =
        out.lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun effectsClient(json: String): HttpClient {
        val engine = MockEngine {
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    private fun effectsLib(json: String): GameRuntimeLibrary {
        val client = effectsClient(json)
        return GameRuntimeLibrary(effectManager = EffectManager(client, GameEventBus()))
    }

    private fun fakeInventory(
        items: Map<Int, InventoryItem>,
    ): Pair<InventoryManager, () -> Boolean> {
        var fetched = false
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            override suspend fun fetchInventory() {
                fetched = true
                _state.value = InventoryState(items = items)
            }

            override suspend fun syncCharacterEquipment() { /* no-op */ }
        }
        return inv to { fetched }
    }

    @Test
    fun revision_phase488() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun effects_printsAtCountAndDuration() {
        val json = """{"42":{"name":"Muscular","duration":10}}"""
        val out = outputLib(effectsLib(json), """cli_execute("effects");""")
        val listed = lines(out)
        assertEquals("0 of 3 AT buffs active.", listed.first())
        assertTrue(listed.contains("Muscular (10)"))
    }

    @Test
    fun effects_atSongCountsAndPrefixesNote() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 71,
                name = "The Ode to Booze",
                image = "ode.gif",
                descId = "ode",
                quality = EffectQuality.GOOD,
                attributes = setOf("song"),
                actions = "cast 1 The Ode to Booze",
            ),
        )
        UneffectSkillEffectMap.rebuild()
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 6014,
                name = "The Ode to Booze",
                image = "skill.gif",
                tags = setOf("nc", "effect", "other", "song"),
                mpCost = 50,
                duration = 10,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = true,
            ),
        )
        val json = """{"71":{"name":"The Ode to Booze","duration":10}}"""
        val out = outputLib(effectsLib(json), """cli_execute("effects");""")
        val listed = lines(out)
        assertEquals("1 of 3 AT buffs active.", listed.first())
        assertTrue(listed.contains("♫ The Ode to Booze (10)"))
    }

    @Test
    fun effects_substringFilterKeepsAtLine() {
        val json = """{"42":{"name":"Muscular","duration":10},"7":{"name":"Goofball","duration":5}}"""
        val out = outputLib(effectsLib(json), """cli_execute("effects mus");""")
        val listed = lines(out)
        assertEquals("0 of 3 AT buffs active.", listed.first())
        assertTrue(listed.contains("Muscular (10)"))
        assertFalse(listed.any { it.contains("Goofball") })
    }

    @Test
    fun inv_andInventory_listQuantitiesAndFilter() {
        val items = mapOf(
            4 to InventoryItem(4, "seal tooth", 1, ItemType.OTHER),
            2 to InventoryItem(2, "meat paste", 10, ItemType.OTHER),
        )
        val (inv, wasFetched) = fakeInventory(items)
        val lib = GameRuntimeLibrary(inventoryManager = inv)
        val all = lines(outputLib(lib, """cli_execute("inv");"""))
        assertTrue(wasFetched())
        assertTrue(all.contains("seal tooth"))
        assertTrue(all.contains("meat paste (10)"))
        val fromInventory = lines(outputLib(lib, """cli_execute("inventory");"""))
        assertEquals(all.toSet(), fromInventory.toSet())
        val filtered = lines(outputLib(lib, """cli_execute("inv paste");"""))
        assertTrue(filtered.contains("meat paste (10)"))
        assertFalse(filtered.any { it.contains("seal tooth") })
    }
}
