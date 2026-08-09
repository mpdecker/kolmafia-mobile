package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class GameRuntimeLibraryAshP118Test {

    @Test
    fun refreshStatus_returnsTrueWhenApiSucceeds() {
        val statusJson = Json.encodeToString(
            CharacterApiResponse.serializer(),
            CharacterApiResponse(name = "Player", hp = "75", hpmax = "100"),
        )
        val client = apiClient(statusJson)
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hp = "30", hpmax = "100"))
        val lib = GameRuntimeLibrary(
            character = char,
            characterRequest = CharacterRequest(client),
        )
        assertEquals("true", outputLib(lib, """print(refresh_status());""").trim())
        assertEquals(75, char.state.value.currentHp)
    }

    @Test
    fun refreshStatus_returnsFalseWithoutCharacterRequest() {
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(refresh_status());""").trim())
    }

    @Test
    fun refreshStatus_returnsFalseOnHttpFailure() {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.InternalServerError) })
        val lib = GameRuntimeLibrary(
            character = KoLCharacter(),
            characterRequest = CharacterRequest(client),
        )
        assertEquals("false", outputLib(lib, """print(refresh_status());""").trim())
    }

    @Test
    fun restoreHp_returnsFalseWithoutRecoveryManager() {
        val lib = GameRuntimeLibrary(character = KoLCharacter())
        assertEquals("false", outputLib(lib, """print(restore_hp(50));""").trim())
    }

    @Test
    fun restoreHp_returnsTrueWhenRecoverySucceeds() = kotlinx.coroutines.test.runTest {
        RestoreDatabase.load()
        ItemDatabase.registerForTest(
            ItemData(
                id = 1381,
                name = "aspirin",
                descId = "775883133",
                image = "aspirin.gif",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        var apiHp = 30
        val statusJson = {
            Json.encodeToString(
                CharacterApiResponse.serializer(),
                CharacterApiResponse(hp = apiHp.toString(), hpmax = "100"),
            )
        }
        val client = HttpClient(
            MockEngine { request ->
                when {
                    request.url.encodedPath.contains("api.php") ->
                        respond(
                            statusJson(),
                            HttpStatusCode.OK,
                            headersOf("Content-Type", "application/json"),
                        )
                    request.url.encodedPath.contains("inv_use.php") -> {
                        apiHp = minOf(apiHp + 101, 100)
                        respond("ok", HttpStatusCode.OK)
                    }
                    else -> respond("", HttpStatusCode.OK)
                }
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val bus = GameEventBus()
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(hp = "30", hpmax = "100"))
        val aspirin = InventoryItem(1381, "aspirin", 3, ItemType.OTHER)
        val inv = object : InventoryManager(client, bus) {
            init {
                _state.value = InventoryState(items = mapOf(1381 to aspirin))
            }
        }
        val rm = RecoveryManager(inv, SkillManager(client, SkillCastRequest(client), bus), prefs())
        val lib = GameRuntimeLibrary(
            character = char,
            characterRequest = CharacterRequest(client),
            inventoryManager = inv,
            skillManager = SkillManager(client, SkillCastRequest(client), bus),
            recoveryManager = rm,
        )
        assertEquals("true", outputLib(lib, """print(restore_hp(50));""").trim())
        assertTrue(char.state.value.currentHp >= 50)
    }

    @Test
    fun restoreMp_returnsFalseWithoutRecoveryManager() {
        val lib = GameRuntimeLibrary(character = KoLCharacter())
        assertEquals("false", outputLib(lib, """print(restore_mp(30));""").trim())
    }

    @Test
    fun revision_phase170() {
        assertEquals("phase350", GameRuntimeLibrary.REVISION)
    }

    private fun apiClient(statusJson: String): HttpClient = HttpClient(
        MockEngine {
            respond(
                statusJson,
                HttpStatusCode.OK,
                headersOf("Content-Type", "application/json"),
            )
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun prefs() = Preferences(com.russhwolf.settings.MapSettings())
}
