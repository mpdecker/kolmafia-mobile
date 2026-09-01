package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.outputLib
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.SessionLogger

class UmbrellaKgbRequestTest {

    @Test
    fun setMode_umbrella_postsChoice1466AndWritesPrefOnce() = runTest {
        val requests = mutableListOf<Triple<HttpMethod, String, String>>()
        val client = HttpClient(MockEngine { request ->
            val body = request.body.toByteArray().decodeToString()
            val query = request.url.encodedQuery
            requests += Triple(request.method, request.url.encodedPath, "$query|$body")
            when (request.url.encodedPath) {
                "/inventory.php" -> respond("folding", HttpStatusCode.OK)
                "/choice.php" -> respond(UMBRELLA_BUCKET_HTML, HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        })
        val settings = CountingSettings()
        val preferences = Preferences(settings)
        val request = ModeableRequest(
            client = client,
            choiceRequest = ChoiceRequest(client),
            preferences = preferences,
            inventoryManager = inventory(client, umbrellaCount = 1),
        )

        val result = request.setMode(Modeable.UMBRELLA, "bucket style")

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(requests.any { it.second == "/inventory.php" && it.third.contains("action=useumbrella") })
        val choice = requests.first { it.second == "/choice.php" }
        assertEquals(HttpMethod.Post, choice.first)
        assertForm(choice.third.substringAfter('|'), "whichchoice", "1466")
        assertForm(choice.third.substringAfter('|'), "option", "3")
        assertEquals("bucket style", preferences.getString("umbrellaState", ""))
        assertEquals(1, settings.umbrellaWrites)
    }

    @Test
    fun setMode_umbrella_rejectsUnequippedNonOwnedBeforeHttp() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(UMBRELLA_BUCKET_HTML, HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings()).apply {
            setString("umbrellaState", "broken")
        }
        val request = ModeableRequest(
            client = client,
            choiceRequest = ChoiceRequest(client),
            preferences = preferences,
            inventoryManager = inventory(client, umbrellaCount = 0),
            character = KoLCharacter(),
        )

        val result = request.setMode(Modeable.UMBRELLA, "bucket style")

        assertTrue(result.isFailure)
        assertEquals(0, calls)
        assertEquals("broken", preferences.getString("umbrellaState", ""))
    }

    @Test
    fun setMode_umbrella_allowsEquippedUmbrellaWithoutInventoryCopy() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += request.url.encodedPath
            when (request.url.encodedPath) {
                "/inventory.php" -> respond("folding", HttpStatusCode.OK)
                "/choice.php" -> respond(UMBRELLA_BUCKET_HTML, HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        })
        val preferences = Preferences(MapSettings())
        val character = KoLCharacter().also {
            it.updateEquipment(EquipmentSlot.OFFHAND, "unbreakable umbrella")
        }
        val request = ModeableRequest(
            client = client,
            choiceRequest = ChoiceRequest(client),
            character = character,
            preferences = preferences,
            inventoryManager = inventory(client, umbrellaCount = 0),
            equipmentManager = EquipmentManager(character, inventory(client, umbrellaCount = 0)),
        )

        val result = request.setMode(Modeable.UMBRELLA, "bucket style")

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(requests.contains("/choice.php"))
        assertEquals("bucket style", preferences.getString("umbrellaState", ""))
    }

    @Test
    fun setMode_umbrella_malformedChoiceLeavesPrefUnchanged() = runTest {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls == 1) respond("folding", HttpStatusCode.OK)
            else respond("<html>still folding, no mode text</html>", HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings()).apply {
            setString("umbrellaState", "broken")
        }
        val request = ModeableRequest(
            client = client,
            choiceRequest = ChoiceRequest(client),
            preferences = preferences,
            inventoryManager = inventory(client, umbrellaCount = 1),
        )

        val result = request.setMode(Modeable.UMBRELLA, "bucket style")

        assertTrue(result.isFailure)
        assertEquals("broken", preferences.getString("umbrellaState", ""))
    }

    @Test
    fun kgb_visit_getsPlaceWhichplaceKgb() = runTest {
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        val client = HttpClient(MockEngine { request ->
            requests += request.method to "${request.url.encodedPath}?${request.url.encodedQuery}"
            respond("<html>KGB</html>", HttpStatusCode.OK)
        })
        val request = KgbRequest(client, Preferences(MapSettings()), null)

        val result = request.visit()

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(1, requests.size)
        assertEquals(HttpMethod.Get, requests[0].first)
        assertTrue(requests[0].second.contains("/place.php"), requests[0].second)
        assertTrue(requests[0].second.contains("whichplace=kgb"), requests[0].second)
    }

    @Test
    fun kgb_button_parsesClicksAndRefreshesModifiersOnce() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += "${request.url.encodedPath}?${request.url.encodedQuery}"
            respond(KGB_BUTTON_HTML, HttpStatusCode.OK)
        })
        ModifierDatabase.injectForTest(
            "Item",
            KGB_ITEM_NAME,
            "Critical Hit Percent: +10",
        )
        val preferences = Preferences(MapSettings())
        val request = KgbRequest(client, preferences, null)

        val result = request.button("kgb_button1")
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(request.parseResponse("place.php?whichplace=kgb&action=kgb_button1", KGB_BUTTON_HTML))

        assertTrue(requests[0].contains("/place.php"), requests[0])
        assertTrue(requests[0].contains("whichplace=kgb"), requests[0])
        assertTrue(requests[0].contains("action=kgb_button1"), requests[0])
        assertEquals(2, preferences.getInt("_kgbClicksUsed", 0))
        val mods = ModifierDatabase.getItem(KGB_ITEM_NAME)?.modifiers.orEmpty()
        assertTrue(mods.contains("PvP Fights"), mods)
        assertFalse(mods.contains("Critical Hit Percent"), mods)
    }

    @Test
    fun kgb_dispenser_postsWhichitemAndIncrementsOnAcquire() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += "${request.url.encodedPath}?${request.url.encodedQuery}"
            respond(KGB_DISPENSER_HTML, HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings())
        val request = KgbRequest(client, preferences, null)

        val result = request.dispenser(DISPENSER_ITEM_ID)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(requests[0].contains("/place.php"), requests[0])
        assertTrue(requests[0].contains("whichplace=kgb"), requests[0])
        assertTrue(requests[0].contains("action=kgb_dispenser"), requests[0])
        assertTrue(requests[0].contains("whichitem=$DISPENSER_ITEM_ID"), requests[0])
        assertEquals(1, preferences.getInt("_kgbDispenserUses", 0))
    }

    @Test
    fun kgb_failedResponsesDoNotMutatePrefsOrModifiers() = runTest {
        val client = HttpClient(MockEngine {
            respond("no", HttpStatusCode.InternalServerError)
        })
        ModifierDatabase.injectForTest("Item", KGB_ITEM_NAME, "Critical Hit Percent: +10")
        val preferences = Preferences(MapSettings()).apply {
            setInt("_kgbClicksUsed", 4)
            setInt("_kgbDispenserUses", 1)
        }
        val request = KgbRequest(client, preferences, null)

        assertTrue(request.button("kgb_button1").isFailure)
        assertTrue(request.dispenser(DISPENSER_ITEM_ID).isFailure)

        assertEquals(4, preferences.getInt("_kgbClicksUsed", 0))
        assertEquals(1, preferences.getInt("_kgbDispenserUses", 0))
        assertTrue(
            ModifierDatabase.getItem(KGB_ITEM_NAME)?.modifiers.orEmpty().contains("Critical Hit Percent"),
        )
    }

    @Test
    fun kgb_malformedDispenserDoesNotIncrement() = runTest {
        val client = HttpClient(MockEngine {
            respond("<html>The dispenser whirrs uselessly.</html>", HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings())
        val request = KgbRequest(client, preferences, null)

        val result = request.dispenser(DISPENSER_ITEM_ID)

        assertTrue(result.isFailure)
        assertEquals(0, preferences.getInt("_kgbDispenserUses", 0))
    }

    @Test
    fun kgb_visitHookRoutesPlacePagesIdempotently() {
        val settings = CountingSettings()
        val preferences = Preferences(settings)
        val library = GameRuntimeLibrary(preferences = preferences)
        val url = "place.php?whichplace=kgb&action=kgb_button1"

        library.processVisitResponseHooks(KGB_BUTTON_HTML, url)
        library.processVisitResponseHooks(KGB_BUTTON_HTML, url)

        assertEquals(2, preferences.getInt("_kgbClicksUsed", 0))
        assertEquals(1, settings.kgbClickWrites)
    }

    @Test
    fun cliUmbrella_usesTypedRequestAndPrintsItsFailure() {
        val client = HttpClient(MockEngine { respond("") })
        val fake = object : ModeableRequest(client, ChoiceRequest(client)) {
            override suspend fun setMode(modeable: Modeable, mode: String): Result<Unit> =
                Result.failure(IllegalStateException("typed umbrella failure"))
        }
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()),
            inventoryManager = inventory(client, umbrellaCount = 1),
            modeableRequest = fake,
        )

        val out = outputLib(lib, """cli_execute("umbrella bucket");""")

        assertTrue(out.contains("typed umbrella failure"), out)
    }

    @Test
    fun cliKgb_usesTypedRequestAndPrintsItsFailure() {
        val client = HttpClient(MockEngine { respond("") })
        val fake = object : KgbRequest(client, Preferences(MapSettings()), null) {
            override suspend fun button(action: String): Result<String> =
                Result.failure(IllegalStateException("typed kgb failure"))
        }
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()),
            kgbRequest = fake,
        )

        val out = outputLib(lib, """cli_execute("kgb button kgb_button1");""")

        assertTrue(out.contains("typed kgb failure"), out)
    }

    @Test
    fun cliKgb_statusDoesNotIssueHttp() {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond("", HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()).apply { setInt("_kgbClicksUsed", 5) },
            kgbRequest = KgbRequest(client, Preferences(MapSettings()), null),
        )

        val out = outputLib(lib, """cli_execute("kgb");""")

        assertTrue(out.contains("5"), out)
        assertEquals(0, calls)
    }

    @Test
    fun cliKgb_bareButtonPrintsUsageWithoutHttp() {
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(KGB_BUTTON_HTML, HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()),
            kgbRequest = KgbRequest(client, Preferences(MapSettings()), null),
        )

        val out = outputLib(lib, """cli_execute("kgb button");""")

        assertTrue(out.contains("Usage"), out)
        assertEquals(0, calls)
    }

    @Test
    fun cliKgb_button1_postsKgbButton1() {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += "${request.url.encodedPath}?${request.url.encodedQuery}"
            respond(KGB_BUTTON_HTML, HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()),
            kgbRequest = KgbRequest(client, Preferences(MapSettings()), null),
        )

        outputLib(lib, """cli_execute("kgb button1");""")

        assertEquals(1, requests.size, requests.toString())
        assertTrue(requests[0].contains("action=kgb_button1"), requests[0])
    }

    @Test
    fun cliKgb_buttonSpace1_postsKgbButton1() {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += "${request.url.encodedPath}?${request.url.encodedQuery}"
            respond(KGB_BUTTON_HTML, HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()),
            kgbRequest = KgbRequest(client, Preferences(MapSettings()), null),
        )

        outputLib(lib, """cli_execute("kgb button 1");""")

        assertEquals(1, requests.size, requests.toString())
        assertTrue(requests[0].contains("action=kgb_button1"), requests[0])
    }

    @Test
    fun kgb_malformedButtonHtmlDoesNotSucceedOrSessionLog() = runTest {
        val client = HttpClient(MockEngine {
            respond("<html>still the briefcase, no symphony</html>", HttpStatusCode.OK)
        })
        val preferences = Preferences(MapSettings())
        val logPrefs = Preferences(MapSettings())
        val request = KgbRequest(client, preferences, SessionLogger(logPrefs, GameEventBus()))

        val result = request.button("kgb_button1")

        assertTrue(result.isFailure)
        assertFalse(
            KgbRequest.parseResponse(
                "place.php?whichplace=kgb&action=kgb_button1",
                "<html>still the briefcase, no symphony</html>",
                preferences,
            ),
        )
        assertEquals(0, preferences.getInt("_kgbClicksUsed", 0))
        assertFalse(
            logPrefs.getString(SessionLogger.SESSION_LOG_KEY, "").contains("kgb"),
            logPrefs.getString(SessionLogger.SESSION_LOG_KEY, ""),
        )
    }

    @Test
    fun kgb_failedDispenserWithClickChromeDoesNotCountClicks() = runTest {
        val html = "<html>The dispenser whirrs uselessly.<br>Click click<br></html>"
        val client = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })
        val preferences = Preferences(MapSettings())
        val request = KgbRequest(client, preferences, null)

        val result = request.dispenser(DISPENSER_ITEM_ID)

        assertTrue(result.isFailure)
        assertEquals(0, preferences.getInt("_kgbClicksUsed", 0))
        assertEquals(0, preferences.getInt("_kgbDispenserUses", 0))
    }

    private fun inventory(client: HttpClient, umbrellaCount: Int): InventoryManager =
        InventoryManager(client, GameEventBus()).also {
            if (umbrellaCount > 0) {
                it.applyParsedInventory(
                    mapOf(
                        UMBRELLA_ID to InventoryItem(
                            UMBRELLA_ID,
                            "unbreakable umbrella",
                            umbrellaCount,
                            ItemType.OFFHAND,
                        ),
                    ),
                )
            }
        }

    private fun assertForm(body: String, key: String, expected: String) {
        val actual = Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)
        assertEquals(expected, actual, body)
    }

    private class CountingSettings(
        private val delegate: Settings = MapSettings(),
    ) : Settings by delegate {
        var umbrellaWrites: Int = 0
            private set
        var kgbClickWrites: Int = 0
            private set

        override fun putString(key: String, value: String) {
            if (key == "umbrellaState") umbrellaWrites++
            delegate.putString(key, value)
        }

        override fun putInt(key: String, value: Int) {
            if (key == "_kgbClicksUsed") kgbClickWrites++
            delegate.putInt(key, value)
        }
    }

    companion object {
        private const val UMBRELLA_ID = 10899
        private const val DISPENSER_ITEM_ID = 9498
        private const val KGB_ITEM_NAME = "Kremlin's Greatest Briefcase"
        private const val UMBRELLA_BUCKET_HTML =
            "You dangle by the handle and the umbrella assumes a bucket style."
        private const val KGB_BUTTON_HTML =
            """<br>Click click<br>A symphony of mechanical buzzing and whirring ensues, and your case seems to be... different somehow.
<s>+10% chance of Critical Hit</s><br><br><b>+5 PvP Fights per day</b>"""
        private const val KGB_DISPENSER_HTML = "You acquire an item: can of Minions-Be-Gone"
    }
}
