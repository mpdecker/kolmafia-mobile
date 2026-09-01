package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.di.sharedModule
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AscensionHistoryRequest
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.FleaMarketRequest
import net.sourceforge.kolmafia.request.FleaMarketSellRequest
import net.sourceforge.kolmafia.request.ForeseeRequest
import net.sourceforge.kolmafia.request.HashingViseRequest
import net.sourceforge.kolmafia.request.KgbRequest
import net.sourceforge.kolmafia.request.PizzaCubeRequest
import net.sourceforge.kolmafia.request.PottedTeaTreeRequest
import net.sourceforge.kolmafia.session.AscensionHistoryManager
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class GameRuntimeLibraryHttpResidualCliTest {

    @Test
    fun revisionIsPhase4010() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun helpListsResidualHttpCommandsAndNonGoals() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help");""")
        val lines = helpCommandLines(out)
        for (command in listOf(
            "vise",
            "teatree",
            "umbrella",
            "foresee",
            "kgb",
            "flea",
            "fleamarket",
            "ascensionhistory",
        )) {
            assertTrue(command in lines, "missing help verb $command in: $out")
        }
        assertFalse("pizza" in lines, "pizza CLI is not implemented")
        assertTrue(out.contains("GUI/Relay"), out)
        assertTrue(out.contains("JavaScript"), out)
        assertTrue(out.contains("TCRS dumps"), out)
        assertTrue(out.contains("desktop scripting"), out)
    }

    @Test
    fun helpForeseeAndFleamarketFiltersMatchAliases() {
        val foresee = outputLib(GameRuntimeLibrary(), """cli_execute("help foresee");""")
        assertTrue(foresee.contains("foresee"), foresee)
        val flea = outputLib(GameRuntimeLibrary(), """cli_execute("help fleamarket");""")
        assertTrue(flea.contains("fleamarket"), flea)
        val kgb = outputLib(GameRuntimeLibrary(), """cli_execute("help kgb");""")
        assertTrue(kgb.contains("kgb"), kgb)
    }

    @Test
    fun helpJavascriptTopicStatesNonGoal() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help javascript");""")
        assertTrue(out.contains("JavaScript"), out)
        assertTrue(out.contains("not available"), out)
    }

    @Test
    fun unavailableHttpMessagesUseInjectedNullRequests() {
        val viseInv = invWith(
            mapOf(11826 to InventoryItem(11826, "hashing vise", 1, ItemType.OTHER)),
        )
        val vise = outputLib(
            GameRuntimeLibrary(inventoryManager = viseInv),
            """cli_execute("vise cyburger");""",
        )
        assertTrue(vise.contains("Hashing vise HTTP is not available."), vise)

        val tea = outputLib(
            GameRuntimeLibrary(preferences = Preferences(MapSettings())),
            """cli_execute("teatree shake");""",
        )
        assertTrue(tea.contains("Tea tree HTTP is not available."), tea)

        val foresee = outputLib(
            GameRuntimeLibrary(preferences = Preferences(MapSettings())),
            """cli_execute("foresee 1");""",
        )
        assertTrue(foresee.contains("Foresee HTTP is not available."), foresee)

        val kgb = outputLib(GameRuntimeLibrary(), """cli_execute("kgb");""")
        assertTrue(kgb.contains("KGB request unavailable"), kgb)

        val fleaBuy = outputLib(GameRuntimeLibrary(), """cli_execute("flea buy 1 1");""")
        assertTrue(fleaBuy.contains("Flea Market buy unavailable"), fleaBuy)

        val fleaSell = outputLib(GameRuntimeLibrary(), """cli_execute("fleamarket sell 1 1 10");""")
        assertTrue(fleaSell.contains("Flea Market sell unavailable"), fleaSell)

        val history = outputLib(GameRuntimeLibrary(), """cli_execute("ascensionhistory");""")
        assertTrue(history.contains("Ascension history HTTP unavailable."), history)
    }

    @Test
    fun injectedFakesDriveResidualCliWithoutLiveHttp() {
        val client = HttpClient(MockEngine { respond("") })
        val teaFake = object : PottedTeaTreeRequest(client, CampgroundRequest(client), ChoiceRequest(client)) {
            override suspend fun shake(): Result<String> = Result.success("shook")
        }
        val teaOut = outputLib(
            GameRuntimeLibrary(
                preferences = Preferences(MapSettings()),
                pottedTeaTreeRequest = teaFake,
            ),
            """cli_execute("teatree shake");""",
        )
        assertTrue(teaOut.contains("Shook the potted tea tree."), teaOut)

        val foreseeFake = object : ForeseeRequest(client, ChoiceRequest(client)) {
            override suspend fun foresee(perilId: Int?): Result<String> = Result.success("ok")
        }
        val foreseeOut = outputLib(
            GameRuntimeLibrary(
                preferences = Preferences(MapSettings()),
                foreseeRequest = foreseeFake,
            ),
            """cli_execute("foresee 99");""",
        )
        assertTrue(foreseeOut.contains("Foreseeing peril for 99."), foreseeOut)

        val kgbFake = object : KgbRequest(client, Preferences(MapSettings()), null) {
            override suspend fun button(action: String): Result<String> = Result.success("pressed")
        }
        val kgbOut = outputLib(
            GameRuntimeLibrary(kgbRequest = kgbFake),
            """cli_execute("kgb button 1");""",
        )
        assertTrue(kgbOut.contains("kgb button 1"), kgbOut)

        val buyFake = object : FleaMarketRequest(client, null, null, null) {
            override suspend fun buy(itemId: Int, quantity: Int): Result<String> = Result.success("bought")
        }
        val buyOut = outputLib(
            GameRuntimeLibrary(fleaMarketRequest = buyFake),
            """cli_execute("flea buy 2 123");""",
        )
        assertTrue(buyOut.contains("flea buy 2 123"), buyOut)

        val sellFake = object : FleaMarketSellRequest(client, null, null, null) {
            override suspend fun sell(itemId: Int, quantity: Int, price: Int): Result<String> =
                Result.success("listed")
        }
        val sellOut = outputLib(
            GameRuntimeLibrary(fleaMarketSellRequest = sellFake),
            """cli_execute("fleamarket sell 1 456 10");""",
        )
        assertTrue(sellOut.contains("flea sell 1 456 10"), sellOut)

        val historyFake = object : AscensionHistoryRequest(client, AscensionHistoryManager()) {
            override suspend fun fetch(playerId: Int?) = Result.success(
                listOf(net.sourceforge.kolmafia.request.AscensionRecord(1, "Seal Clubber", "None", 10, 1)),
            )
        }
        val historyOut = outputLib(
            GameRuntimeLibrary(ascensionHistoryRequest = historyFake),
            """cli_execute("ascensionhistory");""",
        )
        assertTrue(historyOut.contains("Seal Clubber"), historyOut)
    }

    @Test
    fun repeatedVisitHooksAreIdempotentForTeaTreeAndKgb() {
        val settings = CountingSettings()
        val preferences = Preferences(settings)
        val library = GameRuntimeLibrary(preferences = preferences)
        val teaUrl = "choice.php?whichchoice=1104&option=1"
        val teaHtml = "You acquire an item: a delicious cup of tea."
        library.processVisitResponseHooks(teaHtml, teaUrl)
        library.processVisitResponseHooks(teaHtml, teaUrl)
        assertTrue(preferences.getBoolean("_pottedTeaTreeUsed", false))
        assertEquals(1, settings.teaTreeWrites)

        val kgbUrl = "place.php?whichplace=kgb&action=kgb_drawer1"
        library.processVisitResponseHooks("<html>opened</html>", kgbUrl)
        library.processVisitResponseHooks("<html>opened</html>", kgbUrl)
        assertTrue(preferences.getBoolean("_kgbRightDrawerUsed", false))
        assertEquals(1, settings.kgbDrawerWrites)
    }

    @Test
    fun requestLoggerNamesResidualHttpActions() {
        val preferences = Preferences(MapSettings())
        val logger = SessionLogger(preferences, GameEventBus())
        RequestLogger.currentRound = { 0 }
        ChoiceCombatAshState.reset()

        RequestLogger.registerRequest("choice.php?whichchoice=1558&option=1", logger, preferences)
        assertEquals("Foresee", logger.recentLines().last())

        RequestLogger.registerRequest("choice.php?whichchoice=1466&option=1", logger, preferences)
        assertEquals("Umbrella", logger.recentLines().last())

        RequestLogger.registerRequest("place.php?whichplace=kgb&action=kgb_button1", logger, preferences)
        assertEquals("kgb kgb_button1", logger.recentLines().last())

        RequestLogger.registerRequest("campground.php?action=pizza", logger, preferences)
        assertEquals("pizza", logger.recentLines().last())

        RequestLogger.registerRequest("ascensionhistory.php?back=self", logger, preferences)
        assertEquals("ascension history", logger.recentLines().last())
    }

    @Test
    fun sharedModuleResolvesResidualHttpRequests() {
        val app = koinApplication {
            allowOverride(true)
            modules(
                module { single<Settings> { MapSettings() } },
                sharedModule,
                module {
                    single<HttpClient> { HttpClient(MockEngine { respond("") }) }
                },
            )
        }
        val koin = app.koin
        assertNotNull(koin.get<KgbRequest>())
        assertNotNull(koin.get<AscensionHistoryRequest>())
        assertNotNull(koin.get<AscensionHistoryManager>())
        val client = koin.get<HttpClient>()
        val choice = koin.get<ChoiceRequest>()
        val hashing = HashingViseRequest(client, choice)
        val tea = PottedTeaTreeRequest(client, CampgroundRequest(client), choice)
        val foresee = ForeseeRequest(client, choice)
        val pizza = PizzaCubeRequest(client, null, koin.get(), koin.get())
        val fleaBuy = FleaMarketRequest(client, null, koin.get(), koin.get(), koin.get())
        val fleaSell = FleaMarketSellRequest(client, null, koin.get(), koin.get(), koin.get())
        val library = GameRuntimeLibrary(
            hashingViseRequest = hashing,
            pottedTeaTreeRequest = tea,
            foreseeRequest = foresee,
            kgbRequest = koin.get(),
            pizzaCubeRequest = pizza,
            fleaMarketRequest = fleaBuy,
            fleaMarketSellRequest = fleaSell,
            ascensionHistoryRequest = koin.get(),
        )
        assertNotNull(library.hashingViseRequest)
        assertNotNull(library.pottedTeaTreeRequest)
        assertNotNull(library.foreseeRequest)
        assertNotNull(library.kgbRequest)
        assertNotNull(library.pizzaCubeRequest)
        assertNotNull(library.fleaMarketRequest)
        assertNotNull(library.fleaMarketSellRequest)
        assertNotNull(library.ascensionHistoryRequest)
    }

    private fun helpCommandLines(output: String): Set<String> =
        output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private fun invWith(items: Map<Int, InventoryItem>): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(InventoryState(items = items))
            override val state = flow.asStateFlow()
        }

    private class CountingSettings(
        private val delegate: Settings = MapSettings(),
    ) : Settings by delegate {
        var teaTreeWrites: Int = 0
            private set
        var kgbDrawerWrites: Int = 0
            private set

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "_pottedTeaTreeUsed" -> teaTreeWrites++
                "_kgbRightDrawerUsed" -> kgbDrawerWrites++
            }
            delegate.putBoolean(key, value)
        }
    }
}
