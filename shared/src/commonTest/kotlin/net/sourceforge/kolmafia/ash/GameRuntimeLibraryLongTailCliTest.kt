package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
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
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.data.RestoreData
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.data.RestoreType
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.mood.MoodRemovalKnownSources
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.request.ManageStoreRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.session.EventHistory
import net.sourceforge.kolmafia.session.EventHistoryTest
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.item.RetrieveItemService

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
        RestoreDatabase.resetForTest()
        EventHistory.resetForTest()
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

    @Test
    fun burn_zombiecore_isSilentNoOp() {
        val char = KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(path = "Zombie Slayer"),
        )
        val lib = GameRuntimeLibrary(character = char)
        val out = outputLib(lib, """cli_execute("burn extra");""")
        assertEquals("", out)
    }

    @Test
    fun burn_extra_withNoSkills_completesSilently() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("burn extra");""")
        assertEquals("", out)
    }

    @Test
    fun kitchen_withoutBadMoon_printsUnavailable() {
        val char = KoLCharacter()
        char.setZodiacSign("Mongoose")
        val lib = GameRuntimeLibrary(character = char)
        val out = outputLib(lib, """cli_execute("kitchen imp ale");""")
        assertEquals("Hell's Kitchen not available.", out)
    }

    @Test
    fun mallsell_put_callsAddItem() {
        ItemDatabase.registerForTest(
            ItemData(501, "pail", "d", "img", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null),
        )
        val adds = mutableListOf<Triple<Int, Int, Int>>()
        val store = object : ManageStoreRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun addItem(
                itemId: Int,
                price: Int,
                limit: Int,
                quantity: Int,
                fromStorage: Boolean,
            ): Result<String> {
                adds += Triple(itemId, price, limit)
                return Result.success("ok")
            }
        }
        val lib = GameRuntimeLibrary(gameDatabase = GameDatabase(), manageStoreRequest = store)
        outputLib(lib, """cli_execute("mallsell pail @ 20 limit 5");""")
        assertEquals(listOf(Triple(501, 20, 5)), adds)
    }

    @Test
    fun stickers_equipsEmptySlot() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        77 to InventoryItem(77, "upset sticker", 1, ItemType.OTHER),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
            override suspend fun equipItem(item: InventoryItem, slot: String): Result<Unit> {
                equipCalls += item.name to slot
                return Result.success(Unit)
            }
        }
        val lib = GameRuntimeLibrary(inventoryManager = inv, character = KoLCharacter())
        outputLib(lib, """cli_execute("stickers upset");""")
        assertEquals(listOf("upset sticker" to EquipmentSlot.STICKER1.apiKey), equipCalls)
    }

    @Test
    fun condition_clear_clearsGoals() {
        val goals = GoalManager()
        goals.setMeatGoal(100)
        goals.addItemGoalByName("brain")
        val lib = GameRuntimeLibrary(goalManager = goals)
        outputLib(lib, """cli_execute("condition clear");""")
        assertTrue(goals.allGoalsAsStrings().isEmpty())
    }

    @Test
    fun refresh_unknown_printsError() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("refresh nope");""")
        assertEquals("nope cannot be refreshed.", out)
    }

    @Test
    fun refresh_inv_fetchesInventory() {
        var fetched = 0
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            override suspend fun fetchInventory() {
                fetched++
            }
            override suspend fun syncCharacterEquipment() {}
        }
        val lib = GameRuntimeLibrary(inventoryManager = inv)
        outputLib(lib, """cli_execute("refresh inv");""")
        assertEquals(1, fetched)
    }

    @Test
    fun refresh_shop_refreshesPrices() {
        var refreshed = 0
        val store = object : ManageStoreRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun refreshPrices(): Result<String> {
                refreshed++
                return Result.success("ok")
            }
        }
        val lib = GameRuntimeLibrary(manageStoreRequest = store)
        outputLib(lib, """cli_execute("refresh shop");""")
        assertEquals(1, refreshed)
    }

    @Test
    fun refresh_bare_doesNotError() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("refresh");""")
        assertEquals("", out)
    }

    @Test
    fun echo_timestamp_printsCalendarDay() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("echo timestamp");""")
        assertEquals(KolGameHolidayCalendar.getCalendarDayAsString(), out)
        assertTrue(out != "timestamp")
    }

    @Test
    fun mpitems_countsListedRestores() {
        RestoreDatabase.resetForTest()
        RestoreDatabase.registerForTest(
            RestoreData(
                name = "soda water",
                type = RestoreType.ITEM,
                hpMinExpr = "0",
                hpMaxExpr = "0",
                mpMinExpr = "4",
                mpMaxExpr = "8",
                advCost = 0,
                usesLeftExpr = "",
                notes = "",
            ),
        )
        RestoreDatabase.registerForTest(
            RestoreData(
                name = "phonics down",
                type = RestoreType.ITEM,
                hpMinExpr = "0",
                hpMaxExpr = "0",
                mpMinExpr = "10",
                mpMaxExpr = "20",
                advCost = 0,
                usesLeftExpr = "",
                notes = "",
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(1001, "soda water", "d", "img", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(1002, "phonics down", "d", "img", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null),
        )
        val p = Preferences(MapSettings())
        p.setString("mpAutoRecoveryItems", "soda water")
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        1001 to InventoryItem(1001, "soda water", 3, ItemType.OTHER),
                        1002 to InventoryItem(1002, "phonics down", 9, ItemType.OTHER),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }
        val lib = GameRuntimeLibrary(
            preferences = p,
            inventoryManager = inv,
            gameDatabase = GameDatabase(),
        )
        val out = outputLib(lib, """cli_execute("mpitems");""")
        assertEquals("3 mana restores remaining.", out)
    }

    @Test
    fun terminal_enquiry_fam_postsChoice1191() {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val p = Preferences(MapSettings())
        p.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
        val lib = GameRuntimeLibrary(httpClient = client, preferences = p)
        outputLib(lib, """cli_execute("terminal enquiry fam");""")
        assertTrue(bodies.any { it.contains("action=terminal") }, bodies.toString())
        assertTrue(
            bodies.any { it.contains("whichchoice=1191") && it.contains("enquiry") },
            bodies.toString(),
        )
    }

    @Test
    fun terminal_educate_digit_postsChoice1191() {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val p = Preferences(MapSettings())
        p.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
        val lib = GameRuntimeLibrary(httpClient = client, preferences = p)
        outputLib(lib, """cli_execute("terminal educate digit");""")
        assertTrue(
            bodies.any { it.contains("whichchoice=1191") && it.contains("educate") },
            bodies.toString(),
        )
    }

    @Test
    fun terminal_extrude_booze_incrementsUses() {
        val p = Preferences(MapSettings())
        p.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            preferences = p,
        )
        outputLib(lib, """cli_execute("terminal extrude booze");""")
        assertEquals(1, p.getInt("_sourceTerminalExtrudes", 0))
    }

    @Test
    fun terminal_extrude_limit_printsError() {
        val p = Preferences(MapSettings())
        p.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
        p.setInt("_sourceTerminalExtrudes", 3)
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            preferences = p,
        )
        val out = outputLib(lib, """cli_execute("terminal extrude booze");""")
        assertTrue(out.contains("Source Terminal extrude limit reached"), out)
    }

    @Test
    fun spacegate_destination_random_postsChoice1235() {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val p = Preferences(MapSettings())
        p.setBoolean("spacegateAlways", true)
        val lib = GameRuntimeLibrary(
            httpClient = client,
            choiceRequest = ChoiceRequest(client),
            preferences = p,
        )
        outputLib(lib, """cli_execute("spacegate destination random");""")
        assertTrue(bodies.any { it.contains("action=sg_Terminal") }, bodies.toString())
        assertTrue(
            bodies.any { it.contains("whichchoice=1235") && it.contains("option=3") },
            bodies.toString(),
        )
    }

    @Test
    fun spacegate_destination_blockedWhenCoordinatesSet() {
        val p = Preferences(MapSettings())
        p.setBoolean("spacegateAlways", true)
        p.setString("_spacegateCoordinates", "ABCDEFG")
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val lib = GameRuntimeLibrary(
            httpClient = client,
            choiceRequest = ChoiceRequest(client),
            preferences = p,
        )
        val out = outputLib(lib, """cli_execute("spacegate destination random");""")
        assertTrue(out.contains("You've already chosen a destination today"), out)
    }

    @Test
    fun text_charpane_printsStrippedHtml() {
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            urls += request.url.toString()
            respond("<html><body>Hello <b>world</b></body></html>", HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(httpClient = client)
        val out = outputLib(lib, """cli_execute("text charpane.php");""")
        assertTrue(urls.any { it.contains("charpane.php") }, urls.toString())
        assertTrue(out.contains("Hello"), out)
        assertTrue(out.contains("world"), out)
        assertTrue(!out.contains("<b>"), out)
    }

    @Test
    fun unmatched_php_visitsWithoutCliEcho() {
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            urls += request.url.toString()
            respond("<html>ok</html>", HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(httpClient = client)
        val out = outputLib(lib, """cli_execute("guild.php");""")
        assertTrue(urls.any { it.contains("guild.php") }, urls.toString())
        assertTrue(!out.contains("[cli]"), out)
    }

    @Test
    fun acquire_withoutQty_retrievesOne() {
        ItemDatabase.registerForTest(
            ItemData(100, "seal tooth", "d", "img", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null),
        )
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val retrieve = object : RetrieveItemService(
            inventoryManager = null,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = GameDatabase(),
        ) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int {
                retrieved += itemId to qty
                return qty
            }
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = GameDatabase(),
            retrieveItemService = retrieve,
        )
        outputLib(lib, """cli_execute("acquire seal tooth");""")
        assertEquals(listOf(100 to 1), retrieved)
    }

    @Test
    fun acquire_commaList_retrievesEach() {
        ItemDatabase.registerForTest(
            ItemData(101, "item a", "d", "img", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(102, "item b", "d", "img", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null),
        )
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val retrieve = object : RetrieveItemService(
            inventoryManager = null,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = GameDatabase(),
        ) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int {
                retrieved += itemId to qty
                return qty
            }
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = GameDatabase(),
            retrieveItemService = retrieve,
        )
        outputLib(lib, """cli_execute("acquire 2 item a, 1 item b");""")
        assertEquals(listOf(101 to 2, 102 to 1), retrieved)
    }

    @Test
    fun acquire_withQty_stillRetrievesCount() {
        ItemDatabase.registerForTest(
            ItemData(103, "seal tooth", "d", "img", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null),
        )
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val retrieve = object : RetrieveItemService(
            inventoryManager = null,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = GameDatabase(),
        ) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int {
                retrieved += itemId to qty
                return qty
            }
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = GameDatabase(),
            retrieveItemService = retrieve,
        )
        outputLib(lib, """cli_execute("acquire 3 seal tooth");""")
        assertEquals(listOf(103 to 3), retrieved)
    }

    @Test
    fun counters_add_writesRelayCounter() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("counters add 5 Foo");""")
        val raw = p.getString(TurnCounter.PREF_KEY, "")
        assertTrue(raw.contains("Foo"), raw)
        assertTrue(out.contains("Foo"), out)
    }

    @Test
    fun counters_stop_removesRelayCounter() {
        val p = Preferences(MapSettings())
        TurnCounter.startCounting(p, 0, 5, "Foo", "watch.gif")
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        outputLib(lib, """cli_execute("counters stop Foo");""")
        assertEquals("", p.getString(TurnCounter.PREF_KEY, ""))
    }

    @Test
    fun counters_clear_emptiesRelayCounters() {
        val p = Preferences(MapSettings())
        TurnCounter.startCounting(p, 0, 5, "Foo", "watch.gif")
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        outputLib(lib, """cli_execute("counters clear");""")
        assertEquals("", p.getString(TurnCounter.PREF_KEY, ""))
    }

    @Test
    fun counters_bare_stillDumpsPrefCounters() {
        val p = Preferences(MapSettings())
        p.setInt("counter_fights", 3)
        p.registerCounterName("fights")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("counters");""")
        assertTrue(out.contains("fights: 3"), out)
    }

    @Test
    fun campground_spinningwheel_postsAction() {
        val bodies = mutableListOf<String>()
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            urls += request.url.toString()
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(httpClient = client, character = KoLCharacter())
        outputLib(lib, """cli_execute("campground spinningwheel");""")
        assertTrue(urls.any { it.contains("campground.php") }, urls.toString())
        assertTrue(bodies.any { it.contains("action=spinningwheel") }, bodies.toString())
    }

    @Test
    fun campground_rest_postsAction() {
        val bodies = mutableListOf<String>()
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            urls += request.url.toString()
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(httpClient = client, character = KoLCharacter())
        outputLib(lib, """cli_execute("campground rest");""")
        assertTrue(urls.any { it.contains("campground.php") }, urls.toString())
        assertTrue(bodies.any { it.contains("action=rest") }, bodies.toString())
    }

    @Test
    fun campground_rest_chateau_printsUnavailable() {
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(MockEngine { respond("ok") }),
            character = KoLCharacter(),
        )
        val out = outputLib(lib, """cli_execute("campground rest chateau");""")
        assertTrue(out.contains("campground rest is not available"), out)
    }

    @Test
    fun campground_vault3_stillUsesSpaPath() {
        val urls = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            urls += request.url.toString()
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(httpClient = client, character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("campground vault3");""")
        assertTrue(out.contains("Vault 3 is only available in Nuclear Autumn."), out)
        assertTrue(bodies.none { it.contains("action=vault3") }, bodies.toString())
    }

    @Test
    fun restore_hp_usesHpRestoreItem() {
        RestoreDatabase.registerForTest(
            RestoreData(
                name = "aspirin",
                type = RestoreType.ITEM,
                hpMinExpr = "10",
                hpMaxExpr = "20",
                mpMinExpr = "0",
                mpMaxExpr = "0",
                advCost = 0,
                usesLeftExpr = "",
                notes = "",
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(1381, "aspirin", "d", "img", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            respond("ok", HttpStatusCode.OK)
        })
        val bus = GameEventBus()
        val char = KoLCharacter()
        char.updateHpMp(30, 100, 50, 50)
        val inv = object : InventoryManager(client, bus) {
            init {
                _state.value = InventoryState(
                    items = mapOf(1381 to InventoryItem(1381, "aspirin", 3, ItemType.OTHER)),
                )
            }
        }
        val skills = SkillManager(client, SkillCastRequest(client), bus)
        val rm = RecoveryManager(inv, skills, Preferences(MapSettings()))
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            skillManager = skills,
            recoveryManager = rm,
        )
        outputLib(lib, """cli_execute("restore hp");""")
        assertTrue(paths.any { it.contains("inv_use.php") }, paths.toString())
    }

    @Test
    fun restore_mp_usesMpRestoreItem() {
        RestoreDatabase.registerForTest(
            RestoreData(
                name = "soda water",
                type = RestoreType.ITEM,
                hpMinExpr = "0",
                hpMaxExpr = "0",
                mpMinExpr = "4",
                mpMaxExpr = "8",
                advCost = 0,
                usesLeftExpr = "",
                notes = "",
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(1001, "soda water", "d", "img", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            respond("ok", HttpStatusCode.OK)
        })
        val bus = GameEventBus()
        val char = KoLCharacter()
        char.updateHpMp(100, 100, 10, 50)
        val inv = object : InventoryManager(client, bus) {
            init {
                _state.value = InventoryState(
                    items = mapOf(1001 to InventoryItem(1001, "soda water", 5, ItemType.OTHER)),
                )
            }
        }
        val skills = SkillManager(client, SkillCastRequest(client), bus)
        val rm = RecoveryManager(inv, skills, Preferences(MapSettings()))
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            skillManager = skills,
            recoveryManager = rm,
        )
        outputLib(lib, """cli_execute("restore mp");""")
        assertTrue(paths.any { it.contains("inv_use.php") }, paths.toString())
    }

    @Test
    fun restore_bare_doesNotEchoUnknown() {
        val lib = GameRuntimeLibrary(
            character = KoLCharacter(),
            recoveryManager = RecoveryManager(
                InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()),
                SkillManager(
                    HttpClient(MockEngine { respond("ok") }),
                    SkillCastRequest(HttpClient(MockEngine { respond("ok") })),
                    GameEventBus(),
                ),
                Preferences(MapSettings()),
            ),
        )
        val out = outputLib(lib, """cli_execute("restore");""")
        assertTrue(!out.contains("[cli]"), out)
    }

    @Test
    fun choice_extraField_postsForm() {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(
            httpClient = client,
            choiceRequest = ChoiceRequest(client),
            questDatabase = QuestDatabase(p),
            preferences = p,
        )
        outputLib(lib, """cli_execute("choice 1235 2 word=ABCDEFG");""")
        assertTrue(
            bodies.any {
                it.contains("whichchoice=1235") && it.contains("option=2") && it.contains("word=ABCDEFG")
            },
            bodies.toString(),
        )
    }

    @Test
    fun choice_always_writesPref() {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(
            httpClient = client,
            choiceRequest = ChoiceRequest(client),
            questDatabase = QuestDatabase(p),
            preferences = p,
        )
        val out = outputLib(lib, """cli_execute("choice 1235 2 word=ABCDEFG always");""")
        assertEquals("2&word=ABCDEFG", p.getString("choiceAdventure1235", ""))
        assertTrue(out.contains("choiceAdventure1235 => 2&word=ABCDEFG"), out)
    }

    @Test
    fun counters_warn_stripsLocStar() {
        val p = Preferences(MapSettings())
        TurnCounter.startCounting(p, 0, 5, "Foo loc=*", "watch.gif")
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        outputLib(lib, """cli_execute("counters warn Foo");""")
        val raw = p.getString(TurnCounter.PREF_KEY, "")
        assertTrue(raw.contains("Foo"), raw)
        assertTrue(!raw.contains("loc=*"), raw)
    }

    @Test
    fun counters_nowarn_appendsLocStar() {
        val p = Preferences(MapSettings())
        TurnCounter.startCounting(p, 0, 5, "Foo", "watch.gif")
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        outputLib(lib, """cli_execute("counters nowarn Foo");""")
        assertTrue(p.getString(TurnCounter.PREF_KEY, "").contains("Foo loc=*"))
    }

    @Test
    fun restores_all_listsRegistered() {
        RestoreDatabase.registerForTest(
            RestoreData("grog", RestoreType.ITEM, "10", "20", "0", "0", 0, "", "notes"),
        )
        ItemDatabase.registerForTest(
            ItemData(201, "grog", "d", "img", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("restores all");""")
        assertTrue(out.contains("grog"), out)
        assertTrue(out.contains("ITEM"), out)
    }

    @Test
    fun restores_available_omitsZeroInventory() {
        RestoreDatabase.registerForTest(
            RestoreData("grog", RestoreType.ITEM, "10", "20", "0", "0", 0, "", ""),
        )
        RestoreDatabase.registerForTest(
            RestoreData("tonic", RestoreType.ITEM, "0", "0", "4", "8", 0, "", ""),
        )
        ItemDatabase.registerForTest(
            ItemData(201, "grog", "d", "img", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(202, "tonic", "d", "img", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        val client = HttpClient(MockEngine { respond("ok") })
        val inv = object : InventoryManager(client, GameEventBus()) {
            init {
                _state.value = InventoryState(
                    items = mapOf(201 to InventoryItem(201, "grog", 2, ItemType.OTHER)),
                )
            }
        }
        val lib = GameRuntimeLibrary(inventoryManager = inv)
        val out = outputLib(lib, """cli_execute("restores available");""")
        assertTrue(out.contains("grog"), out)
        assertTrue(!out.contains("tonic"), out)
    }

    @Test
    fun restores_invalid_printsValidParameters() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("restores bananas");""")
        assertTrue(out.contains("Valid parameters are all, available or obtainable"), out)
    }

    @Test
    fun ashref_print_includesPrint() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("ashref print");""")
        assertTrue(out.contains("print("), out)
    }

    @Test
    fun ashref_unknown_printsNothingMatching() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("ashref nosuchfunctionxyz");""")
        assertTrue(!out.contains("print("), out)
        assertTrue(!out.contains("nosuchfunctionxyz"), out)
    }

    @Test
    fun insults_known_printsRetortAndOdds() {
        val p = Preferences(MapSettings())
        p.setBoolean("lastPirateInsult1", true)
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("insults");""")
        assertTrue(out.contains("Obviously neither your tongue nor your wit is sharp enough for the job."), out)
        assertTrue(out.contains("chance of winning at Insult Beer Pong"), out)
        assertTrue(out.contains("1 insult"), out)
    }

    @Test
    fun acquire_checkOnly_doesNotRetrieve() {
        ItemDatabase.registerForTest(
            ItemData(100, "seal tooth", "d", "img", ItemPrimaryUse.NONE, emptySet(), emptySet(), 0, null),
        )
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val retrieve = object : RetrieveItemService(
            inventoryManager = null,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = GameDatabase(),
        ) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int {
                retrieved += itemId to qty
                return qty
            }
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = GameDatabase(),
            retrieveItemService = retrieve,
        )
        val out = outputLib(lib, """cli_execute("acquire? seal tooth");""")
        assertEquals(emptyList(), retrieved)
        assertTrue(out.contains("seal tooth: fail") || out.contains("seal tooth: have"), out)
    }

    @Test
    fun rest_postsAction() {
        val bodies = mutableListOf<String>()
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            urls += request.url.toString()
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(httpClient = client, character = KoLCharacter())
        outputLib(lib, """cli_execute("rest");""")
        assertTrue(urls.any { it.contains("campground.php") }, urls.toString())
        assertTrue(bodies.any { it.contains("action=rest") }, bodies.toString())
    }

    @Test
    fun rest_chateau_printsUnavailable() {
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(MockEngine { respond("ok") }),
            character = KoLCharacter(),
        )
        val out = outputLib(lib, """cli_execute("rest chateau");""")
        assertTrue(out.contains("campground rest is not available"), out)
    }

    @Test
    fun restores_all_stillListsAfterRestAlias() {
        RestoreDatabase.registerForTest(
            RestoreData("grog", RestoreType.ITEM, "10", "20", "0", "0", 0, "", "notes"),
        )
        ItemDatabase.registerForTest(
            ItemData(201, "grog", "d", "img", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("restores all");""")
        assertTrue(out.contains("grog"), out)
    }

    @Test
    fun second_equipsOffhand() {
        assertOffhandAlias("second")
    }

    @Test
    fun hold_equipsOffhand() {
        assertOffhandAlias("hold")
    }

    @Test
    fun dualwield_equipsOffhand() {
        assertOffhandAlias("dualwield")
    }

    @Test
    fun equip_offHand_token_accepted() {
        assertOffhandAlias("equip off-hand seal tooth", commandIsFull = true)
    }

    @Test
    fun colorecho_printsTextNotColor() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("colorecho red hello");""")
        assertTrue(out.contains("hello"), out)
        assertTrue(!out.contains("red hello"), out)
    }

    @Test
    fun events_printsParsedText_andClearEmpties() {
        EventHistory.checkForNewEvents(EventHistoryTest.ORANGE_EVENTS_HTML)
        val lib = GameRuntimeLibrary.forTesting()
        val listed = outputLib(lib, """cli_execute("events");""")
        assertTrue(listed.contains("You found a thing."), listed)
        outputLib(lib, """cli_execute("events clear");""")
        val after = outputLib(lib, """cli_execute("events");""")
        assertTrue(!after.contains("You found a thing."), after)
    }

    @Test
    fun prefref_listsSetPref() {
        val p = Preferences(MapSettings())
        p.setString("someKey", "someValue")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("prefref someKey");""")
        assertTrue(out.contains("someKey"), out)
        assertTrue(out.contains("someValue"), out)
    }

    @Test
    fun prefref_unknown_printsNothingMatching() {
        val p = Preferences(MapSettings())
        p.setString("otherKey", "v")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("prefref nosuch");""")
        assertTrue(!out.contains("otherKey"), out)
        assertTrue(!out.contains("nosuch"), out)
    }

    @Test
    fun poolskill_printsEstimateFromInebrietyAndPref() {
        val p = Preferences(MapSettings())
        p.setInt("poolSkill", 3)
        p.setInt("poolSharkCount", 0)
        val char = KoLCharacter().also { it.updateConsumables(0, 5, 0) }
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        val out = outputLib(lib, """cli_execute("poolskill");""")
        assertTrue(out.contains("Pool Skill is estimated at : 8."), out)
        assertTrue(out.contains("5 from having 5 inebriety"), out)
        assertTrue(out.contains("3 hustling training"), out)
    }

    private fun assertOffhandAlias(command: String, commandIsFull: Boolean = false) {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("ok", HttpStatusCode.OK)
        })
        val inv = object : InventoryManager(client, GameEventBus()) {
            init {
                _state.value = InventoryState(
                    items = mapOf(2 to InventoryItem(2, "seal tooth", 1, ItemType.OTHER)),
                )
            }
        }
        val lib = GameRuntimeLibrary(inventoryManager = inv)
        val cli = if (commandIsFull) command else "$command seal tooth"
        outputLib(lib, """cli_execute("$cli");""")
        assertTrue(bodies.any { it.contains("slot=offhand") }, bodies.toString())
        assertTrue(bodies.any { it.contains("whichitem=2") }, bodies.toString())
    }
}
