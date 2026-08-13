package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.BountyData
import net.sourceforge.kolmafia.data.BountyDatabase
import net.sourceforge.kolmafia.data.BountyType
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionMayoQueue
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.HolidayNames
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.mood.MoodRemovalKnownSources
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UseItemRequest

class GameRuntimeLibraryLongTailCliTest {

    @AfterTest
    fun tearDown() {
        GameRuntimeLibrary.waitMillis = { kotlinx.coroutines.delay(it) }
        HolidayNames.clearOverride()
        FoldGroupDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        BountyDatabase.resetForTest()
        EffectDatabase.resetForTest()
        MoodRemovalKnownSources.clear()
    }

    @Test
    fun wait_printsCompletedWithoutSleeping() {
        GameRuntimeLibrary.waitMillis = { }
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("wait 3");""")
        assertEquals("Waiting completed.", out)
    }

    @Test
    fun waitq_isQuiet() {
        GameRuntimeLibrary.waitMillis = { }
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("waitq 2");""")
        assertEquals("", out)
    }

    @Test
    fun olfact_monster_writesPref() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("olfact monster groar");""")
        assertEquals("monster groar", p.getString("autoOlfact", ""))
        assertTrue(out.contains("autoOlfact: monster groar"))
    }

    @Test
    fun putty_none_clearsPref() {
        val p = Preferences(MapSettings())
        p.setString("autoPutty", "monster foo")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("putty none");""")
        assertEquals("", p.getString("autoPutty", "x"))
        assertTrue(out.contains("autoPutty is disabled."))
    }

    @Test
    fun holiday_override_printsAndAshReads() {
        val lib = GameRuntimeLibrary.forTesting()
        val cli = outputLib(lib, """cli_execute("holiday Bill 1");""")
        assertTrue(cli.contains("Bill 1"), cli)
        val ash = outputLib(lib, """print(holiday());""")
        assertTrue(ash.contains("Bill 1"), ash)
    }

    @Test
    fun banishes_listsActive() {
        val p = Preferences(MapSettings())
        val mgr = BanishManager(p)
        mgr.banishMonster("dairy goat", Banisher.SNOKEBOMB, currentTurn = 10)
        val char = KoLCharacter()
        char.setCurrentRun(12)
        val lib = GameRuntimeLibrary(banishManager = mgr, character = char)
        val out = outputLib(lib, """cli_execute("banishes");""")
        assertTrue(out.contains("dairy goat"), out)
        assertTrue(out.contains("snokebomb"), out)
        assertTrue(out.contains("28") || out.contains("Turns Left"), out)
    }

    @Test
    fun recipe_printsCraftTypeAndIngredients() {
        ItemDatabase.registerForTest(
            ItemData(9001, "cli toast", "", "", ItemPrimaryUse.FOOD, emptySet(), emptySet(), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(9002, "cli bread", "", "", ItemPrimaryUse.FOOD, emptySet(), emptySet(), 0, null),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "cli toast",
                resultQuantity = 1,
                methods = setOf("COOK"),
                ingredients = listOf(ConcoctionIngredient("cli bread", 1)),
            ),
        )
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("recipe cli toast");""")
        assertTrue(out.contains("cli toast"), out)
        assertTrue(out.contains("Cooking") || out.contains("cli bread"), out)
    }

    @Test
    fun fold_cli_invokesInvUse() {
        FoldGroupDatabase.registerGroupForTest(FoldGroup(0, listOf("cli-fold-a", "cli-fold-b")))
        ItemDatabase.registerForTest(
            ItemData(41, "cli-fold-a", "", "", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(42, "cli-fold-b", "", "", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls += request.url.toString()
            respond("ok", HttpStatusCode.OK)
        }
        val inv = object : InventoryManager(HttpClient(engine), GameEventBus()) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(42 to InventoryItem(42, "cli-fold-b", 1, ItemType.OTHER))),
            ).asStateFlow()
        }
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(engine),
            inventoryManager = inv,
        )
        val out = outputLib(lib, """cli_execute("fold cli-fold-a");""")
        assertTrue(out.contains("Folded") || urls.any { it.contains("whichitem=42") }, out + urls)
    }

    @Test
    fun ash_printsReturnedValue() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("ash 1 + 1");""")
        assertTrue(out.contains("Returned: 2"), out)
    }

    @Test
    fun ashq_isQuiet() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("ashq 1 + 1");""")
        assertEquals("", out)
    }

    @Test
    fun abort_message_stopsScript() {
        val lib = GameRuntimeLibrary.forTesting()
        val runtime = AshRuntime(lib)
        val ex = runCatching {
            runtime.execute(AshParser().parse("""cli_execute("abort done"); print("after");"""))
        }.exceptionOrNull()
        assertTrue(ex is ScriptException, ex?.toString() ?: "no exception")
        val out = runtime.output.toString()
        assertTrue(out.contains("done"), out)
        assertTrue(!out.contains("after"), out)
    }

    @Test
    fun autoattack_none_printsDisabled() {
        val char = KoLCharacter()
        char.setAutoAttackAction(1)
        val lib = GameRuntimeLibrary(character = char)
        val out = outputLib(lib, """cli_execute("aa none");""")
        assertEquals(0, char.state.value.autoAttackAction)
        assertTrue(out.contains("disabled"), out)
    }

    @Test
    fun bounty_listsCurrentFromPrefs() {
        val p = Preferences(MapSettings())
        p.setString("currentEasyBountyItem", "bean-ruy:2")
        BountyDatabase.registerForTest(
            BountyData(
                name = "bean-ruy",
                plural = "beans-ruy",
                type = BountyType.EASY,
                image = "",
                count = 5,
                monster = "beanbat",
                bestLocation = "The Beanbat Chamber",
            ),
        )
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("bounty");""")
        assertTrue(out.contains("Easy"), out)
        assertTrue(out.contains("beanbat") || out.contains("beans-ruy"), out)
        BountyDatabase.resetForTest()
    }

    @Test
    fun saber_withoutItem_printsNeed() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("saber mp");""")
        assertTrue(out.contains("Fourth of May Cosplay Saber"), out)
    }

    @Test
    fun snapper_withoutFamiliar_printsNeed() {
        val lib = GameRuntimeLibrary(character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("snapper beast");""")
        assertTrue(out.contains("Red-Nosed Snapper"), out)
    }

    @Test
    fun eudora_empty_printsCurrentFromPref() {
        val p = Preferences(MapSettings())
        p.setString("eudora", "Pen Pal")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("eudora");""")
        assertTrue(out.contains("Pen Pal"), out)
    }

    @Test
    fun correspondent_switch_setsPref() {
        val p = Preferences(MapSettings())
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val lib = GameRuntimeLibrary(preferences = p, httpClient = HttpClient(engine))
        val out = outputLib(lib, """cli_execute("correspondent xi");""")
        assertEquals("Xi Receiver Unit", p.getString("eudora", ""))
        assertTrue(out.contains("Switched to Xi Receiver Unit"), out)
    }

    @Test
    fun mayominder_withoutClinic_printsNeed() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(lib, """cli_execute("mayominder drunk");""")
        assertTrue(out.contains("Mayo clinic not installed"), out)
    }

    @Test
    fun mayominder_setsChoicePref() {
        val p = Preferences(MapSettings())
        p.setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, ConcoctionMayoQueue.MAYO_CLINIC)
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val client = HttpClient(engine)
        val inv = object : InventoryManager(client, GameEventBus()) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(8285 to InventoryItem(8285, "Mayo Minder", 1, ItemType.OTHER))),
            ).asStateFlow()
        }
        val lib = GameRuntimeLibrary(
            preferences = p,
            httpClient = client,
            inventoryManager = inv,
            useItemRequest = UseItemRequest(client),
            choiceRequest = ChoiceRequest(client),
        )
        val out = outputLib(lib, """cli_execute("mayominder mayodiol");""")
        assertEquals("Mayodiol", p.getString("mayoMinderSetting", ""))
        assertTrue(out.contains("Mayodiol"), out)
    }

    @Test
    fun bang_listsIdentifications() {
        val p = Preferences(MapSettings())
        p.setString("lastBangPotion821", "healing")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("bang");""")
        assertTrue(out.contains("bubbly: healing"), out)
        assertTrue(out.contains("cloudy: unidentified"), out)
    }

    @Test
    fun vials_listsIdentifications() {
        val p = Preferences(MapSettings())
        p.setString("lastSlimeVial3885", "strength")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("vials");""")
        assertTrue(out.contains("red: strength"), out)
        assertTrue(out.contains("brown: unidentified"), out)
    }

    @Test
    fun up_dispatchesDefaultAction() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 9001,
                name = "Cli Boost Effect",
                image = "",
                descId = "cli-boost",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "aa none",
            ),
        )
        val char = KoLCharacter()
        char.setAutoAttackAction(1)
        val lib = GameRuntimeLibrary(character = char)
        val out = outputLib(lib, """cli_execute("up Cli Boost Effect");""")
        assertEquals(0, char.state.value.autoAttackAction)
        assertTrue(out.contains("disabled"), out)
    }

    @Test
    fun up_unknownEffect_printsError() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("up not-a-real-effect");""")
        assertTrue(out.contains("Unknown effect"), out)
    }

    @Test
    fun spoon_withoutItem_printsNeed() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("spoon Wallaby");""")
        assertTrue(out.contains("hewn moon-rune spoon"), out)
    }

    @Test
    fun spoon_alreadyTuned_printsError() {
        val p = Preferences(MapSettings())
        p.setBoolean("moonTuned", true)
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val inv = object : InventoryManager(HttpClient(engine), GameEventBus()) {
            override val state = MutableStateFlow(
                InventoryState(
                    items = mapOf(10254 to InventoryItem(10254, "hewn moon-rune spoon", 1, ItemType.OTHER)),
                ),
            ).asStateFlow()
        }
        val lib = GameRuntimeLibrary(preferences = p, inventoryManager = inv)
        val out = outputLib(lib, """cli_execute("spoon Wallaby");""")
        assertTrue(out.contains("already tuned"), out)
    }

    @Test
    fun spoon_tunesMoonSign() {
        val p = Preferences(MapSettings())
        val char = KoLCharacter()
        char.setZodiacSign("Mongoose")
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls += request.url.toString()
            respond("ok", HttpStatusCode.OK)
        }
        val inv = object : InventoryManager(HttpClient(engine), GameEventBus()) {
            override val state = MutableStateFlow(
                InventoryState(
                    items = mapOf(10254 to InventoryItem(10254, "hewn moon-rune spoon", 1, ItemType.OTHER)),
                ),
            ).asStateFlow()
        }
        val lib = GameRuntimeLibrary(
            preferences = p,
            character = char,
            inventoryManager = inv,
            httpClient = HttpClient(engine),
        )
        val out = outputLib(lib, """cli_execute("spoon Wallaby");""")
        assertTrue(p.getBoolean("moonTuned", false))
        assertEquals("Wallaby", char.state.value.zodiacSign)
        assertTrue(out.contains("Tuning moon to Wallaby"), out)
        assertTrue(urls.any { it.contains("whichsign=2") }, urls.toString())
    }

    @Test
    fun ccs_noArg_printsCurrent() {
        val p = Preferences(MapSettings())
        p.setString("combatMacro", "default")
        p.setString("battleAction", "attack")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("ccs");""")
        assertTrue(out.contains("CCS is default"), out)
        assertTrue(out.contains("battle action is currently set to attack"), out)
    }

    @Test
    fun dusty_listsBottleTypes() {
        ItemDatabase.registerForTest(
            ItemData(2271, "dusty bottle of Merlot", "d1", "img", ItemPrimaryUse.DRINK, emptySet(), emptySet(), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(2272, "dusty bottle of Port", "d2", "img", ItemPrimaryUse.DRINK, emptySet(), emptySet(), 0, null),
        )
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("dusty");""")
        assertTrue(out.contains("dusty bottle of Merlot: average"), out)
        assertTrue(out.contains("dusty bottle of Port: vinegar"), out)
        assertTrue(out.contains("spooky"), out)
        assertTrue(out.contains("great"), out)
        assertTrue(out.contains("glassy"), out)
        assertTrue(out.contains("bad"), out)
    }

    @Test
    fun hermit_noArg_printsCloverCount() {
        val hermit = object : net.sourceforge.kolmafia.request.HermitRequest(
            HttpClient(MockEngine { respond("3 left in stock for today", HttpStatusCode.OK) }),
        ) {}
        val lib = GameRuntimeLibrary(hermitRequest = hermit)
        val out = outputLib(lib, """cli_execute("hermit");""")
        assertEquals("The Hermit has 3 clovers available today.", out)
    }

    @Test
    fun chips_unknownFlavor_printsGate() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("chips chocolate");""")
        assertTrue(out.contains("You can't buy 'chocolate' chips"), out)
    }

    @Test
    fun sofa_withoutAdventures_printsGate() {
        val char = KoLCharacter()
        char.updateAdventuresLeft(0)
        val lib = GameRuntimeLibrary(character = char)
        val out = outputLib(lib, """cli_execute("sleep 1");""")
        assertTrue(out.contains("Insufficient adventures"), out)
    }

    @Test
    fun crimbotree_empty_printsDays() {
        val p = Preferences(MapSettings())
        p.setInt("crimboTreeDays", 4)
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("crimbotree");""")
        assertEquals("Check back in 4 days.", out)
    }

    @Test
    fun version_printsRevision() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("version");""")
        assertEquals("KoLmafia Mobile ${GameRuntimeLibrary.REVISION}", out)
    }

    @Test
    fun whatif_withoutMaximizer_printsUnavailable() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("whatif muscle");""")
        assertEquals("Maximizer unavailable", out)
    }
}
