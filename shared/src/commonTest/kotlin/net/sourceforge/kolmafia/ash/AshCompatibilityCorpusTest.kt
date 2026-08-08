package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.combat.EncounterModifierPipeline
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.DripArmoryPrefs
import net.sourceforge.kolmafia.concoction.StillsAvailability
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.DescriptionCache
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.data.TCRSDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.ZapRequest
import net.sourceforge.kolmafia.session.WandDiscovery
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import net.sourceforge.kolmafia.session.AdventureSpentTracker
import net.sourceforge.kolmafia.session.DreadKissesTracker
import net.sourceforge.kolmafia.session.WildfireCampManager
import net.sourceforge.kolmafia.session.PastaThrall
import net.sourceforge.kolmafia.thrall.PastaThrallManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AshCompatibilityCorpusTest {

    @Test
    fun corpus_basicLocationAndCollectionSnippets() {
        val p = prefs()
        p.setString(Preferences.LAST_LOCATION, "The Spooky Forest")
        CollectionCache.save(p, Preferences.CACHED_CLOSET, mapOf(42 to 2))
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("The Spooky Forest", outputLib(lib, "print(my_location());"))
        assertEquals("false", outputLib(lib, """print(to_string(pvp_attack("someone")));"""))
        assertEquals("false", outputLib(lib, "print(to_string(ranked_fam()));"))
    }

    @Test
    fun corpus_combatScriptSnippet() {
        val scripts = listOf(
            ScriptEntry("fight", """set_ccs_action("attack;");""", type = ScriptType.COMBAT),
        )
        val p = prefs()
        p.setString(ScriptManager.SCRIPTS_PREF_KEY, Json.encodeToString(scripts))
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("attack;", outputLib(lib, """print(get_ccs_action());"""))
    }

    @Test
    fun corpus_cliHighTrafficSnippets() {
        val p = prefs()
        p.registerCounterName("kills")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Testy",
                    playerid = "99",
                    level = "5",
                    classId = "5",
                    adventures = "12",
                    meat = "1000",
                ),
            )
        }
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        runLib(lib, """cli_execute("counter kills add 3");""")
        assertEquals("3", outputLib(lib, """cli_execute("counter kills");"""))
        assertTrue(outputLib(lib, """cli_execute("show all");""").contains("Testy"))
        assertEquals("That's funny.", outputLib(lib, """cli_execute("joke");""").trim())
        assertFalse(outputLib(lib, """cli_execute("pvp attack rival");""").contains("[cli]"))
        assertEquals("false", outputLib(lib, """cli_execute("is_adventuring");""").trim())
        assertEquals("false", outputLib(lib, """cli_execute("has_queued_commands");""").trim())
        assertEquals("0", outputLib(lib, """print(to_string(my_path_id()));""").trim())
        assertEquals("Muscle", outputLib(lib, """print(modifier_name("Muscle"));""").trim())
    }

    @Test
    fun corpus_coinmasterModifierEntity_live() = runBlocking {
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.load()
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("Dimemaster", outputLib(lib, """print(to_coinmaster("dimemaster"));""").trim())
        assertEquals("", outputLib(lib, """print(to_coinmaster("bogus"));""").trim())
        assertEquals("Muscle Percent", outputLib(lib, """print(to_modifier("Muscle Percent"));""").trim())
        assertEquals("", outputLib(lib, """print(to_modifier("bogus"));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_coinmaster("dmt"))));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_modifier("Meat Drop"))));""").trim())
    }

    @Test
    fun corpus_bountySlotPhylumEntity_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("bean-shaped rock", outputLib(lib, """print(to_bounty("bean-shaped rock"));""").trim())
        assertEquals("", outputLib(lib, """print(to_bounty("bogus"));""").trim())
        assertEquals("acc1", outputLib(lib, """print(to_slot("acc1"));""").trim())
        assertEquals("undead", outputLib(lib, """print(to_phylum("undead"));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_slot("hat"))));""").trim())
    }

    @Test
    fun corpus_classElementEntity_live() {
        val lib = GameRuntimeLibrary()
        assertEquals("Seal Clubber", outputLib(lib, """print(to_class("Seal Clubber"));""").trim())
        assertEquals("", outputLib(lib, """print(to_class("bogus"));""").trim())
        assertEquals("cold", outputLib(lib, """print(to_element("cold"));""").trim())
        assertEquals("", outputLib(lib, """print(to_element("bogus"));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_class("Pastamancer"))));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_element("stench"))));""").trim())
    }

    @Test
    fun corpus_monsterPathThrallEntity_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("huge mosquito", outputLib(lib, """print(to_monster("huge mosquito"));""").trim())
        assertEquals("", outputLib(lib, """print(to_monster("bogus"));""").trim())
        assertEquals("Dark Gyffte", outputLib(lib, """print(to_path("Dark Gyffte"));""").trim())
        assertEquals("Lasagmbie", outputLib(lib, """print(to_thrall("Lasagmbie"));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_monster("huge mosquito"))));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_thrall("Lasagmbie"))));""").trim())
    }

    @Test
    fun corpus_locationEntity_live() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "The Spooky Forest",
            outputLib(lib, """print(to_location("spooky forest"));""").trim(),
        )
        assertEquals("The Spooky Forest", outputLib(lib, """print(to_location("20"));""").trim())
        assertEquals("", outputLib(lib, """print(to_location("bogus"));""").trim())
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_location("The Spooky Forest"))));""").trim(),
        )
    }

    @Test
    fun corpus_edServantLevelXp_live() {
        val prefs = prefs()
        net.sourceforge.kolmafia.servant.EdServantState.upsert(
            prefs,
            net.sourceforge.kolmafia.servant.EdServantRecord("Cat", "Hethys", 14, 221),
        )
        val char = KoLCharacter()
        val manager = net.sourceforge.kolmafia.servant.EdServantManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            prefs,
            char,
        )
        val lib = GameRuntimeLibrary(preferences = prefs, character = char, edServantManager = manager)
        assertEquals("14", outputLib(lib, """print(to_servant("Cat")["level"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(have_servant(to_servant("Cat"))));""").trim())
    }

    @Test
    fun corpus_thrallVykeaEntityFields_live() = runBlocking {
        val prefs = prefs()
        prefs.setString(net.sourceforge.kolmafia.session.PastaThrall.prefKey(1), "7,Vampieroghi")
        val db = GameDatabase()
        db.load()
        ModifierDatabase.load()
        val lib = GameRuntimeLibrary(preferences = prefs, gameDatabase = db)
        assertEquals("7", outputLib(lib, """print(to_thrall("Vampieroghi")["level"]);""").trim())
        assertEquals(
            "30.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_vykea("level 3 couch"), "Meat Drop")));""",
            ).trim(),
        )
        assertEquals(
            "Meat Drop: +30",
            outputLib(lib, """print(to_vykea("level 3 couch")["modifiers"]);""").trim(),
        )
    }

    @Test
    fun corpus_myVykeaCompanion_live() = runBlocking {
        val prefs = prefs()
        prefs.setString(
            net.sourceforge.kolmafia.vykea.VykeaCompanionManager.CURRENT_VYKEA_PREF,
            "CHEBLI, the level 5 blood lamp",
        )
        val manager = net.sourceforge.kolmafia.vykea.VykeaCompanionManager(prefs)
        val lib = GameRuntimeLibrary(preferences = prefs, vykeaCompanionManager = manager)
        assertEquals("CHEBLI, the level 5 blood lamp", outputLib(lib, "print(my_vykea_companion());").trim())
        assertEquals("5", outputLib(lib, """print(my_vykea_companion()["level"]);""").trim())
    }

    @Test
    fun corpus_myThrall_live() = runBlocking {
        val prefs = prefs()
        prefs.setString(PastaThrallManager.CURRENT_THRALL_PREF, "Vampieroghi")
        prefs.setString(PastaThrall.prefKey(1), "7,Count Alfredo")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(name = "Test", classId = "3"))
        }
        val manager = PastaThrallManager(prefs, char)
        val lib = GameRuntimeLibrary(preferences = prefs, character = char, pastaThrallManager = manager)
        assertEquals("Vampieroghi", outputLib(lib, "print(my_thrall());").trim())
        assertEquals("7", outputLib(lib, """print(my_thrall()["level"]);""").trim())
    }

    @Test
    fun corpus_monsterEntityFields_live() = runBlocking {
        val prefs = prefs()
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(preferences = prefs, gameDatabase = db)
        assertEquals("1341", outputLib(lib, """print(to_monster("huge mosquito")["id"]);""").trim())
        assertEquals("18", outputLib(lib, """print(to_monster("huge mosquito")["base_hp"]);""").trim())
        assertEquals("bug", outputLib(lib, """print(to_monster("huge mosquito")["phylum"]);""").trim())
        assertEquals("10", outputLib(lib, """print(last_monster()["min_meat"]);""").trim())
    }

    @Test
    fun corpus_locationEntityFields_live() = runBlocking {
        val prefs = prefs()
        prefs.setString(Preferences.LAST_LOCATION, "The Haunted Pantry")
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(preferences = prefs, gameDatabase = db)
        assertEquals("113", outputLib(lib, """print(to_location("The Haunted Pantry")["id"]);""").trim())
        assertEquals("indoor", outputLib(lib, """print(to_location("The Haunted Pantry")["environment"]);""").trim())
        assertEquals("113", outputLib(lib, """print(my_location()["id"]);""").trim())
    }

    @Test
    fun corpus_pathEntityFields_live() = runBlocking {
        val prefs = prefs()
        prefs.setInt("youRobotPoints", 5)
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("41", outputLib(lib, """print(to_path("You, Robot")["id"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(to_path("You, Robot")["familiars"]));""").trim())
        assertEquals("5", outputLib(lib, """print(to_path("You, Robot")["points"]);""").trim())
    }

    @Test
    fun corpus_itemEntityFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("3", outputLib(lib, """print(to_item("acceptable bagel")["fullness"]);""").trim())
        assertEquals("good", outputLib(lib, """print(to_item("acceptable bagel")["quality"]);""").trim())
        assertEquals("13", outputLib(lib, """print(to_item("mime army challenge coin")["levelreq"]);""").trim())
    }

    @Test
    fun corpus_itemRestoreFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("101", outputLib(lib, """print(to_item("aspirin")["minhp"]);""").trim())
        assertEquals("101", outputLib(lib, """print(to_item("aspirin")["maxhp"]);""").trim())
        assertEquals("30", outputLib(lib, """print(to_item("ancient pills")["minmp"]);""").trim())
    }

    @Test
    fun corpus_itemFlagFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("true", outputLib(lib, """print(to_item("acceptable bagel")["tradeable"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_item("ten-leaf clover")["multi"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_item("seal-clubbing club")["smithable"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_item("seal tooth")["combat_reusable"]);""").trim())
    }

    @Test
    fun corpus_itemMetadataFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("Unspaded", outputLib(lib, """print(to_item("candy rations")["notes"]);""").trim())
        assertEquals("simple", outputLib(lib, """print(to_item("tamarind-flavored chewing gum")["candy_type"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_item("fancy chocolate")["chocolate"]);""").trim())
        assertEquals("unspaded", outputLib(lib, """print(to_item("hard rock candy")["candy_type"]);""").trim())
    }

    @Test
    fun corpus_itemDailyUsesLeft_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = net.sourceforge.kolmafia.character.KoLCharacter().also {
            it.updateFromApiResponse(
                net.sourceforge.kolmafia.character.CharacterApiResponse(
                    fullness = "10",
                    stomachsize = "15",
                ),
            )
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            character = char,
            preferences = prefs(),
        )
        assertEquals("5", outputLib(lib, """print(to_item("hot wing")["dailyusesleft"]);""").trim())
        assertEquals(
            "2147483647",
            outputLib(lib, """print(to_item("ten-leaf clover")["dailyusesleft"]);""").trim(),
        )
    }

    @Test
    fun corpus_itemDailyUsesLeftV2_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = net.sourceforge.kolmafia.character.KoLCharacter().also {
            it.updateFromApiResponse(
                net.sourceforge.kolmafia.character.CharacterApiResponse(
                    fullness = "0",
                    stomachsize = "15",
                ),
            )
        }
        val mgr = net.sourceforge.kolmafia.adventure.AdventureManager(
            adventureRequest = net.sourceforge.kolmafia.adventure.AdventureRequest(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { respond("") }),
            ),
            fightRequest = net.sourceforge.kolmafia.adventure.FightRequest(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { respond("") }),
            ),
            choiceRequest = net.sourceforge.kolmafia.adventure.ChoiceRequest(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { respond("") }),
            ),
            characterRequest = net.sourceforge.kolmafia.request.CharacterRequest(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { respond("") }),
            ),
            character = char,
            preferences = prefs(),
            eventBus = net.sourceforge.kolmafia.event.GameEventBus(),
        )
        mgr.testSetCombatFlags(inMultiFight = true, fightFollowsChoice = false)
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            character = char,
            preferences = prefs(),
            adventureManager = mgr,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_item("hot wing")["dailyusesleft"]);""").trim(),
        )
    }

    @Test
    fun corpus_skillBracketFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("combat", outputLib(lib, """print(to_skill("CLEESH")["type"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_skill("Liver of Steel")["passive"]);""").trim())
        assertEquals(
            "seal clubber",
            outputLib(lib, """print(to_skill("Thrust-Smack")["class"]);""").trim(),
        )
    }

    @Test
    fun corpus_faxbotCanFaxbot_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val faxDb = net.sourceforge.kolmafia.faxbot.FaxBotDatabase.instance
        faxDb.clearForTest()
        val bot = net.sourceforge.kolmafia.faxbot.FaxBot("onlyfax", 1)
        bot.addMonsters(
            listOf(
                net.sourceforge.kolmafia.faxbot.FaxBotMonster(
                    "rockfish", "rockfish", "rockfish", "fish", 848,
                ),
            ),
        )
        faxDb.registerBotForTest(bot)
        val probe = object : net.sourceforge.kolmafia.chat.ChatProbe(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun isPlayerOnline(player: String): Boolean = true
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            faxBotDatabase = faxDb,
            chatProbe = probe,
        )
        assertEquals("true", outputLib(lib, """print(can_faxbot(to_monster("rockfish")));""").trim())
        assertEquals(
            "true",
            outputLib(lib, """print(can_faxbot(to_monster("rockfish"), "onlyfax"));""").trim(),
        )
    }

    @Test
    fun corpus_isOnline_live() {
        val probe = object : net.sourceforge.kolmafia.chat.ChatProbe(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun isPlayerOnline(player: String): Boolean =
                player.equals("onlyfax", ignoreCase = true)
        }
        val lib = GameRuntimeLibrary(chatProbe = probe)
        assertEquals("true", outputLib(lib, """print(is_online("onlyfax"));""").trim())
        assertEquals("false", outputLib(lib, """print(is_online("easyfax"));""").trim())
    }

    @Test
    fun corpus_slash_count_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val itemId = db.item("seal tooth")!!.id
        val probe = object : net.sourceforge.kolmafia.chat.ChatProbe(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun slashCount(id: Int): Int = if (id == itemId) 9 else 0
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, chatProbe = probe)
        assertEquals("9", outputLib(lib, """print(slash_count(to_item("seal tooth")));""").trim())
    }

    @Test
    fun corpus_chat_clan_live() {
        val sender = object : net.sourceforge.kolmafia.chat.ChatSender(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            var lastChannel: String? = null
            var lastMessage: String? = null
            override suspend fun send(channel: String, message: String): Result<Unit> {
                lastChannel = channel
                lastMessage = message
                return Result.success(Unit)
            }
        }
        val lib = GameRuntimeLibrary(chatSender = sender)
        outputLib(lib, """chat_clan("corpus ping");""")
        assertEquals("clan", sender.lastChannel)
        assertEquals("corpus ping", sender.lastMessage)
    }

    @Test
    fun corpus_who_clan_live() {
        val probe = object : net.sourceforge.kolmafia.chat.ChatProbe(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun whoClan(): Map<String, Boolean> =
                mapOf("onlyfax" to true, "easyfax" to false)
        }
        val lib = GameRuntimeLibrary(chatProbe = probe)
        assertEquals("true", outputLib(lib, """print(contains_key(who_clan(), "onlyfax"));""").trim())
        assertEquals("true", outputLib(lib, """print(who_clan()["onlyfax"]);""").trim())
    }

    @Test
    fun corpus_get_player_id_live() {
        val probe = object : net.sourceforge.kolmafia.chat.ChatProbe(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun lookupPlayerId(name: String): String =
                if (name.equals("onlyfax", ignoreCase = true)) "12345" else name
        }
        val lib = GameRuntimeLibrary(chatProbe = probe)
        assertEquals("12345", outputLib(lib, """print(get_player_id("onlyfax"));""").trim())
    }

    @Test
    fun corpus_chat_notify_live() = runTest {
        val manager = net.sourceforge.kolmafia.chat.ChatManager()
        val lib = GameRuntimeLibrary(chatManager = manager)
        outputLib(lib, """chat_notify("corpus hello", "red");""")
        val messages = manager.channelFlow(net.sourceforge.kolmafia.chat.ChatManager.EVENTS_CHANNEL).first()
        assertEquals(1, messages.size)
        assertEquals("corpus hello", messages[0].content)
        assertEquals("red", messages[0].color)
    }

    @Test
    fun corpus_effectBracketFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("2", outputLib(lib, """print(to_effect("Sleepy")["id"]);""").trim())
        assertEquals("bad", outputLib(lib, """print(to_effect("Sleepy")["quality"]);""").trim())
        assertEquals(
            "use 1 decorative fountain",
            outputLib(lib, """print(to_effect("Sleepy")["default"]);""").trim(),
        )
        assertEquals("true", outputLib(lib, """print(to_effect("Aloysius' Antiphon of Aptitude")["song"]);""").trim())
        assertEquals("1", outputLib(lib, """print(to_effect("Synthesis: Hot")["candy_tier"]);""").trim())
    }

    @Test
    fun corpus_familiarBracketFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("1", outputLib(lib, """print(to_familiar("Mosquito")["id"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_familiar("Mosquito")["combat"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_familiar("Mosquito")["physical_damage"]);""").trim())
        assertEquals("Bite", outputLib(lib, """print(to_familiar("Angry Goat")["poke_move_1"]);""").trim())
    }

    @Test
    fun corpus_bountyBracketFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("easy", outputLib(lib, """print(to_bounty("bean-shaped rock")["type"]);""").trim())
        assertEquals("low", outputLib(lib, """print(to_bounty("bean-shaped rock")["kol_internal_type"]);""").trim())
        assertEquals("12", outputLib(lib, """print(to_bounty("bean-shaped rock")["number"]);""").trim())
        assertEquals("beanbat", outputLib(lib, """print(to_bounty("bean-shaped rock")["monster"]);""").trim())
    }

    @Test
    fun corpus_phylumElementBracketFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("beastflavor.gif", outputLib(lib, """print(to_phylum("beast")["image"]);""").trim())
        assertEquals("snowflake.gif", outputLib(lib, """print(to_element("cold")["image"]);""").trim())
    }

    @Test
    fun corpus_modifierClassBracketFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("numeric", outputLib(lib, """print(to_modifier("Muscle")["type"]);""").trim())
        assertEquals("1", outputLib(lib, """print(to_class("Seal Clubber")["id"]);""").trim())
    }

    @Test
    fun corpus_coinmasterBracketFields_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("dimemaster", outputLib(lib, """print(to_coinmaster("dimemaster")["nickname"]);""").trim())
        assertEquals(
            "Shore Inc. Ship Trip Scrip",
            outputLib(lib, """print(to_coinmaster("shore")["token"]);""").trim(),
        )
    }

    @Test
    fun corpus_interactiveAshSoftDefaults_live() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("true", outputLib(lib, """print(to_string(user_confirm("ok?")));""").trim())
        assertEquals("mobot", outputLib(lib, """print(user_prompt("name?", 5, "mobot"));""").trim())
    }

    @Test
    fun corpus_userNotifySoftDefaults_live() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("ok", outputLib(lib, """
            user_notify("heads up");
            user_notify("hidden only", true);
            print("ok");
        """.trimIndent()).trim())
    }

    @Test
    fun corpus_requestBuff_live() {
        val db = net.sourceforge.kolmafia.buffbot.BuffBotDatabase.forTest(
            costs = listOf(
                net.sourceforge.kolmafia.buffbot.BuffCost(
                    buffId = 3004,
                    buffName = "Empathy of the Newt",
                    meatCost = 100L,
                    turns = 10,
                ),
            ),
        )
        val sender = net.sourceforge.kolmafia.chat.ChatSender(
            io.ktor.client.HttpClient(
                io.ktor.client.engine.mock.MockEngine {
                    respond("{}", io.ktor.http.HttpStatusCode.OK)
                },
            ),
        )
        val lib = GameRuntimeLibrary(
            buffBotManager = net.sourceforge.kolmafia.buffbot.BuffBotManager(sender, db),
            buffBotDatabase = db,
        )
        assertEquals("true", outputLib(lib, """print(request_buff("OakBot", 3004));""").trim())
        val cliOut = outputLib(lib, """cli_execute("buff OakBot 3004");""")
        assertFalse(cliOut.contains("[cli]"))
        assertTrue(cliOut.contains("Buff request sent to OakBot"))
    }

    @Test
    fun corpus_itemBracketFieldsV6_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("folder2.gif", outputLib(lib, """print(to_item("folder (red)")["smallimage"]);""").trim())
        assertEquals(
            "The Shore, Inc. Gift Shop",
            outputLib(lib, """print(to_item("dinghy plans")["seller"]);""").trim(),
        )
        assertEquals("Dimemaster", outputLib(lib, """print(to_item("beer bong")["buyer"]);""").trim())
        assertEquals(
            "Holiday Weight Gain",
            outputLib(lib, """print(to_item("A Crimbo Carol, Ch. 1")["skill"]);""").trim(),
        )
        assertEquals(
            "concoction of clumsiness",
            outputLib(lib, """print(to_item("fumble formula")["recipe"]);""").trim(),
        )
        assertEquals("Work Ethic", outputLib(lib, """print(to_item("hot wing")["noob_skill"]);""").trim())
    }

    @Test
    fun corpus_itemBracketTcrsName_live() = runBlocking {
        TCRSDatabase.reset()
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "hot wing",
            outputLib(lib, """print(to_item("hot wing")["tcrs_name"]);""").trim(),
        )
        TCRSDatabase.registerForTest(471, "bouncing spicy batwing")
        assertEquals(
            "bouncing spicy batwing",
            outputLib(lib, """print(to_item("hot wing")["tcrs_name"]);""").trim(),
        )
        TCRSDatabase.reset()
    }

    @Test
    fun corpus_locationSessionFields_live() = runBlocking {
        val prefs = prefs()
        val tracker = AdventureSpentTracker(prefs)
        repeat(3) { tracker.addTurn("The Haunted Pantry") }
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            gameDatabase = db,
            adventureSpentTracker = tracker,
        )
        assertEquals("3", outputLib(lib, """print(to_location("The Haunted Pantry")["turns_spent"]);""").trim())
        assertEquals("3", outputLib(lib, """print(my_total_turns_spent());""").trim())
    }

    @Test
    fun corpus_locationKissesWaterFire_live() = runBlocking {
        val prefs = prefs()
        val db = GameDatabase()
        db.load()
        val kisses = DreadKissesTracker(prefs)
        kisses.setKissesForTest("Dreadsylvanian Woods", 2)
        val wildfire = WildfireCampManager(prefs)
        wildfire.setFireLevelForTest("Dreadsylvanian Woods", 4)
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                net.sourceforge.kolmafia.character.CharacterApiResponse(
                    path = net.sourceforge.kolmafia.character.AscensionPath.HEAVY_RAINS.apiName,
                ),
            )
        }
        val lib = GameRuntimeLibrary(
            character = char,
            preferences = prefs,
            gameDatabase = db,
            dreadKissesTracker = kisses,
            wildfireCampManager = wildfire,
        )
        assertEquals("2", outputLib(lib, """print(to_location("Dreadsylvanian Woods")["kisses"]);""").trim())
        assertEquals("2", outputLib(lib, """print(to_location("Dreadsylvanian Woods")["water_level"]);""").trim())
        val fireChar = KoLCharacter().also {
            it.updateFromApiResponse(
                net.sourceforge.kolmafia.character.CharacterApiResponse(
                    path = net.sourceforge.kolmafia.character.AscensionPath.WILDFIRE.apiName,
                ),
            )
        }
        val fireLib = GameRuntimeLibrary(
            character = fireChar,
            preferences = prefs,
            gameDatabase = db,
            wildfireCampManager = wildfire,
        )
        assertEquals("4", outputLib(fireLib, """print(to_location("Dreadsylvanian Woods")["fire_level"]);""").trim())
    }

    @Test
    fun corpus_getMonstersAppearanceRates_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "6",
            outputLib(lib, """print(count(get_monsters(to_location("The Spooky Forest"))));""").trim(),
        )
        assertEquals(
            "15.0",
            outputLib(
                lib,
                """print(to_string(appearance_rates(to_location("The Spooky Forest"))[to_monster("none")]));""",
            ).trim(),
        )
        assertEquals(
            "4",
            outputLib(lib, """print(count(to_monster("spooky vampire")["parts"]));""").trim(),
        )
        assertTrue(
            outputLib(lib, """print(to_monster("spooky vampire")["parts"][1]);""").trim() == "head",
        )
    }

    @Test
    fun corpus_combatAdjustment_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(classId = "5", buffedmox = "0", mox = "0", sign = "Marmot"),
            )
        }
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        assertEquals("0", outputLib(lib, """print(monster_level_adjustment());""").trim())
        assertEquals(
            "10.0",
            outputLib(lib, """print(to_string(elemental_resistance(to_element("cold"))));""").trim(),
        )
        assertTrue(outputLib(lib, """print(expected_damage());""").trim().toLong() > 0)
        assertEquals(
            "spooky",
            net.sourceforge.kolmafia.data.MonsterDatabase
                .getByName("ancient protector spirit")
                ?.attackElement,
        )
    }

    @Test
    fun corpus_dropXpModifiers_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(classId = "3"))
        }
        char.updateEquipment(
            net.sourceforge.kolmafia.character.EquipmentSlot.HAT,
            "googly-ball hat",
        )
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals("3.0", outputLib(lib, """print(to_string(experience_bonus()));""").trim())
        assertEquals("0.0", outputLib(lib, """print(to_string(meat_drop_modifier()));""").trim())
        assertEquals("0.0", outputLib(lib, """print(to_string(item_drop_modifier()));""").trim())
        assertEquals("0.0", outputLib(lib, """print(to_string(initiative_modifier()));""").trim())
    }

    @Test
    fun corpus_monsterCombatStats_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals("16", outputLib(lib, """print(monster_attack());""").trim())
        assertEquals("14", outputLib(lib, """print(monster_defense());""").trim())
        assertEquals("18", outputLib(lib, """print(monster_hp());""").trim())
        assertEquals("20", outputLib(lib, """print(monster_initiative());""").trim())
        assertEquals("bug", outputLib(lib, """print(monster_phylum());""").trim())
        assertEquals(
            "16",
            outputLib(lib, """print(monster_attack(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun corpus_monsterElement_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "Axe Wound")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals("sleaze", outputLib(lib, """print(monster_element());""").trim())
        assertEquals(
            "hot",
            outputLib(lib, """print(to_monster("Axe Wound")["attack_element"]);""").trim(),
        )
        assertEquals(
            "sleaze",
            outputLib(lib, """print(to_monster("Axe Wound")["defense_element"]);""").trim(),
        )
    }

    @Test
    fun corpus_jumpChance_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals("80", outputLib(lib, """print(jump_chance());""").trim())
        assertEquals(
            "60",
            outputLib(lib, """print(jump_chance(to_monster("huge mosquito"), 0, 40));""").trim(),
        )
        val locationExpected = CombatAdjustment.locationJumpChance(
            "The Spooky Forest", 0, 0, 0, 0,
        ) { net.sourceforge.kolmafia.data.MonsterDatabase.getByName(it) }
        assertEquals(
            locationExpected.toString(),
            outputLib(lib, """print(jump_chance(to_location("The Spooky Forest"), 0, 0));""").trim(),
        )
        assertEquals(
            "0",
            outputLib(lib, """print(jump_chance(to_location("Nowhere Land That Does Not Exist")));""").trim(),
        )
        assertEquals(
            "-1",
            outputLib(lib, """print(jump_chance(to_monster("crazy bastard")));""").trim(),
        )
    }

    @Test
    fun corpus_expressionInit_sourceAgent() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "25",
            outputLib(lib, """print(monster_initiative(to_monster("Source Agent")));""").trim(),
        )
        assertEquals(
            "75",
            outputLib(lib, """print(jump_chance(to_monster("Source Agent"), 0, 0));""").trim(),
        )
        prefs.setString("sourceAgentsDefeated", "2")
        assertEquals(
            "75",
            outputLib(lib, """print(monster_initiative(to_monster("Source Agent")));""").trim(),
        )
    }

    @Test
    fun corpus_expressionAtk_sourceAgent() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "30",
            outputLib(lib, """print(monster_attack(to_monster("Source Agent")));""").trim(),
        )
        prefs.setString("sourceAgentsDefeated", "2")
        assertEquals(
            "90",
            outputLib(lib, """print(monster_attack(to_monster("Source Agent")));""").trim(),
        )
    }

    @Test
    fun corpus_expressionDef_sourceAgent() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "30",
            outputLib(lib, """print(monster_defense(to_monster("Source Agent")));""").trim(),
        )
        prefs.setString("sourceAgentsDefeated", "2")
        assertEquals(
            "90",
            outputLib(lib, """print(monster_defense(to_monster("Source Agent")));""").trim(),
        )
    }

    @Test
    fun corpus_expressionHp_sourceAgent() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "40",
            outputLib(lib, """print(monster_hp(to_monster("Source Agent")));""").trim(),
        )
        prefs.setString("sourceAgentsDefeated", "2")
        assertEquals(
            "120",
            outputLib(lib, """print(monster_hp(to_monster("Source Agent")));""").trim(),
        )
    }

    @Test
    fun corpus_scaleStats_amokPutty() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(buffedmox = "50", mox = "50", buffedmus = "50", mus = "50"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "51",
            outputLib(lib, """print(monster_attack(to_monster("amok putty")));""").trim(),
        )
        assertEquals(
            "38",
            outputLib(lib, """print(monster_hp(to_monster("amok putty")));""").trim(),
        )
    }

    @Test
    fun corpus_mlMult_caveBarsNoMl() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(buffedmox = "50", mox = "50", buffedmus = "50", mus = "50"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        // Scale:20 → Atk 70 with ML=0 (MLMult:5 idle until ML applied)
        assertEquals(
            "70",
            outputLib(lib, """print(monster_attack(to_monster("clan of cave bars")));""").trim(),
        )
    }

    @Test
    fun corpus_expressionScale_sausageGoblin() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("_sausageFights", "3")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(buffedmox = "50", mox = "50"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        // Scale 1+2*3=7 → Atk min(57, 10000)=57
        assertEquals(
            "57",
            outputLib(lib, """print(monster_attack(to_monster("sausage goblin")));""").trim(),
        )
    }

    @Test
    fun corpus_baseMainstatExp_guyMadeOfBees() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "20.0",
            outputLib(lib, """print(to_monster("Guy Made Of Bees")["base_mainstat_exp"]);""").trim(),
        )
    }

    @Test
    fun corpus_reduceEnemyDefense_sharpshooterHat() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, "sharpshooter's hat")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "13",
            outputLib(lib, """print(monster_defense(to_monster("huge mosquito")));""").trim(),
        )
    }

    @Test
    fun corpus_physicalResistance_ancientProtector() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "100",
            outputLib(lib, """print(to_monster("ancient protector spirit")["physical_resistance"]);""").trim(),
        )
    }

    @Test
    fun corpus_physicalResistance_mosquitoNoMl() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        // ML=0 → no fight-time boost
        assertEquals(
            "0",
            outputLib(lib, """print(to_monster("huge mosquito")["physical_resistance"]);""").trim(),
        )
    }

    @Test
    fun corpus_rawAttack_mosquito() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "16",
            outputLib(lib, """print(to_monster("huge mosquito")["raw_attack"]);""").trim(),
        )
    }

    @Test
    fun corpus_baseAttack_mosquitoMlZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "16",
            outputLib(lib, """print(to_monster("huge mosquito")["base_attack"]);""").trim(),
        )
    }

    @Test
    fun corpus_monsterAttackElements_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "2",
            outputLib(
                lib,
                """print(count(to_monster("A.M.C. gremlin")["attack_elements"]));""",
            ).trim(),
        )
        assertEquals(
            "hot",
            outputLib(
                lib,
                """print(to_monster("A.M.C. gremlin")["attack_elements"][1]);""",
            ).trim(),
        )
    }

    @Test
    fun corpus_monsterSubTypesImages_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "ghost",
            outputLib(
                lib,
                """print(to_monster("ancient protector spirit")["sub_types"][0]);""",
            ).trim(),
        )
        assertEquals(
            "7",
            outputLib(lib, """print(count(to_monster("Ed the Undying")["images"]));""").trim(),
        )
    }

    @Test
    fun corpus_monsterAttributesRandomModifiers_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val attrs = outputLib(
            lib,
            """print(to_monster("huge mosquito")["attributes"]);""",
        ).trim()
        assert(attrs.startsWith("Atk: 16"))
        assertEquals(
            "0",
            outputLib(
                lib,
                """print(count(to_monster("huge mosquito")["random_modifiers"]));""",
            ).trim(),
        )
    }

    @Test
    fun corpus_beefyBat_beecoreBaseAttack() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(path = "Bees Hate You", kingliberated = "0"),
        )
        val beecoreLib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "40",
            outputLib(
                beecoreLib,
                """print(to_monster("beefy bodyguard bat")["base_attack"]);""",
            ).trim(),
        )
        val defaultLib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "25",
            outputLib(
                defaultLib,
                """print(to_monster("beefy bodyguard bat")["base_attack"]);""",
            ).trim(),
        )
    }

    @Test
    fun corpus_lastMonsterRandomModifiers_live() = runBlocking {
        MonsterStatusTracker.resetLastMonster()
        val db = GameDatabase()
        db.load()
        val template = db.monster("huge mosquito")!!
        MonsterStatusTracker.setNextMonster(template, listOf("huge"))
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, template.name)
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "1",
            outputLib(lib, """print(count(last_monster()["random_modifiers"]));""").trim(),
        )
        assertEquals(
            "36",
            outputLib(lib, """print(last_monster()["base_hp"]);""").trim(),
        )
        assertEquals(
            "18",
            outputLib(lib, """print(to_monster("huge mosquito")["base_hp"]);""").trim(),
        )
        MonsterStatusTracker.resetLastMonster()
    }

    @Test
    fun corpus_lastMonsterMaskPipeline_live() = runBlocking {
        MonsterStatusTracker.resetLastMonster()
        val db = GameDatabase()
        db.load()
        val template = db.monster("Naughty Sorceress")!!
        val modifiers = mutableListOf<String>()
        EncounterModifierPipeline.applyPostOcrs(
            "Naughty Sorceress wearing a Boss Bat mask",
            modifiers,
            EncounterModifierPipeline.EncounterModifierContext(
                familiarId = 0,
                ascensionPath = AscensionPath.DISGUISES_DELIMIT,
            ),
        )
        MonsterStatusTracker.setNextMonster(template, modifiers)
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, template.name)
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "Boss Bat mask",
            outputLib(lib, """print(last_monster()["random_modifiers"][0]);""").trim(),
        )
        MonsterStatusTracker.resetLastMonster()
    }

    @Test
    fun corpus_monsterSprinkles_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1",
            outputLib(lib, """print(to_monster("gingerbread pigeon")["min_sprinkles"]);""").trim(),
        )
        assertEquals(
            "3",
            outputLib(lib, """print(to_monster("gingerbread pigeon")["max_sprinkles"]);""").trim(),
        )
    }

    @Test
    fun corpus_monsterFact_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = net.sourceforge.kolmafia.character.KoLCharacter().also {
            it.updateFromApiResponse(
                net.sourceforge.kolmafia.character.CharacterApiResponse(classId = "1"),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "effect",
            outputLib(lib, """print(to_monster("huge mosquito")["fact_type"]);""").trim(),
        )
        assertEquals(
            "",
            outputLib(lib, """print(to_monster("nonexistent critter")["fact"]);""").trim(),
        )
    }

    @Test
    fun corpus_monsterMetadata_poisonGroup() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "none",
            outputLib(lib, """print(to_monster("huge mosquito")["poison"]);""").trim(),
        )
        assertEquals(
            "1",
            outputLib(lib, """print(to_monster("huge mosquito")["group"]);""").trim(),
        )
    }

    @Test
    fun corpus_meatItemDrops_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals("10", outputLib(lib, """print(meat_drop());""").trim())
        assertEquals("-1", outputLib(lib, """print(meat_drop(to_monster("none")));""").trim())
        assertEquals(
            "30.0",
            outputLib(
                lib,
                """print(to_string(item_drops(to_monster("huge mosquito"))[to_item("delicious swamp muck")]));""",
            ).trim(),
        )
    }

    @Test
    fun corpus_hitMiss_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "huge mosquito")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                net.sourceforge.kolmafia.character.CharacterApiResponse(
                    buffedmox = "40",
                    buffedmus = "10",
                ),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        assertEquals("true", outputLib(lib, """print(to_string(will_usually_dodge()));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(will_usually_miss()));""").trim())
        assertEquals("Muscle", outputLib(lib, """print(current_hit_stat());""").trim())
    }

    @Test
    fun corpus_allMonstersWithId_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "true",
            outputLib(
                lib,
                """print(to_string(all_monsters_with_id()[to_monster("huge mosquito")]));""",
            ).trim(),
        )
    }

    @Test
    fun corpus_servantVykeaEntity_live() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("Cat", outputLib(lib, """print(to_servant("Cat"));""").trim())
        assertEquals("", outputLib(lib, """print(to_servant("skeleton"));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_servant("Cat"))));""").trim())
        assertEquals("level 3 couch", outputLib(lib, """print(to_vykea("level 3 couch"));""").trim())
        assertEquals(
            "30.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_vykea("level 3 couch"), "Meat Drop")));""",
            ).trim(),
        )
    }

    @Test
    fun corpus_locationModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val penalty = outputLib(
            lib,
            """print(to_string(numeric_modifier(to_location("The Briny Deeps"), "Item Drop Penalty")));""",
        )
        assertEquals("-25.0", penalty)
    }

    @Test
    fun corpus_pathModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1.0",
            outputLib(lib, """print(to_string(numeric_modifier(to_path("You, Robot"), "Energy")));"""),
        )
    }

    @Test
    fun corpus_maximizerModifierSnippet() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val out = outputLib(
            lib,
            """
            print(modifier_name("Item Drop Penalty"));
            print(to_string(numeric_modifier(to_location("The Briny Deeps"), "Item Drop Penalty")));
            """.trimIndent(),
        )
        assertTrue(out.contains("Item Drop Penalty"))
        assertTrue(out.contains("-25"))
    }

    @Test
    fun corpus_outfitModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "25.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier("Outfit:Antique Nutcracker Outfit", "Muscle")));""",
            ),
        )
    }

    @Test
    fun corpus_signModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier("Sign:Marmot", "Cold Resistance")));""",
            ),
        )
    }

    @Test
    fun corpus_currentNumericModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        char.updateEquipment(net.sourceforge.kolmafia.character.EquipmentSlot.ACC1, "Jarlsberg's earring")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "10.0",
            outputLib(lib, """print(to_string(numeric_modifier("Mysticality")));"""),
        )
    }

    @Test
    fun corpus_elementModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(sign = "Marmot"))
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "1.0",
            outputLib(lib, """print(to_string(numeric_modifier(to_element("cold"), "Cold Resistance")));"""),
        )
    }

    @Test
    fun corpus_classModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "Muscle",
            outputLib(lib, """print(string_modifier(to_class("Seal Clubber"), "Stat Tuning"));""").trim(),
        )
    }

    @Test
    fun corpus_statEntity_live() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_stat("SubMoxie"))));""").trim())
        assertEquals("stat", outputLib(lib, """print(type_of(to_stat("Mysticality")));""").trim())
        assertEquals(
            "0.0",
            outputLib(lib, """print(to_string(numeric_modifier(to_stat("Moxie"), "Moxie")));""").trim(),
        )
    }

    @Test
    fun corpus_numericsModifier_effectDuration() = runBlocking {
        val effectsJson = """{"1":{"name":"Adventurerlike","duration":10}}"""
        val client = HttpClient(MockEngine {
            respond(effectsJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val effectMgr = EffectManager(client, net.sourceforge.kolmafia.event.GameEventBus())
        effectMgr.fetchEffects()
        val lib = GameRuntimeLibrary(effectManager = effectMgr)
        assertTrue(outputLib(lib, """print(to_string(numerics_modifier("Effect Duration")));""").contains("10"))
    }

    @Test
    fun corpus_availableAmount_lolStorageGate() = runBlocking {
        val itemId = 9101
        val item = net.sourceforge.kolmafia.data.ItemData(
            id = itemId,
            name = "corpus lol weapon",
            descId = "desc9101",
            image = "w.gif",
            primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.WEAPON,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : GameDatabase() {
            override fun item(name: String): net.sourceforge.kolmafia.data.ItemData? =
                if (name.equals("corpus lol weapon", ignoreCase = true)) item else null
            override fun item(id: Int): net.sourceforge.kolmafia.data.ItemData? = if (id == itemId) item else null
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Legacy of Loathing"))
        }
        val inv = object : net.sourceforge.kolmafia.inventory.InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                net.sourceforge.kolmafia.inventory.InventoryState(
                    items = mapOf(
                        itemId to net.sourceforge.kolmafia.inventory.InventoryItem(
                            itemId, "corpus lol weapon", 2, net.sourceforge.kolmafia.inventory.ItemType.OTHER,
                        ),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }
        val storage = object : net.sourceforge.kolmafia.request.StorageRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun fetchRawContents(): Map<Int, Int> = mapOf(itemId to 7)
        }
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            storageRequest = storage,
            gameDatabase = db,
        )
        assertEquals(
            "2",
            outputLib(lib, """print(to_string(available_amount(to_item("corpus lol weapon"))));"""),
        )
    }

    @Test
    fun corpus_availableAmount_trendyGate() = runBlocking {
        net.sourceforge.kolmafia.request.TrendyRequest.parseResponse(
            """
            <tr class="expired">
            <td>2004-12</td><td>Items</td><td>corpus trendy snack</td></tr>
            """.trimIndent(),
        )
        val itemId = 9102
        val item = net.sourceforge.kolmafia.data.ItemData(
            id = itemId,
            name = "corpus trendy snack",
            descId = "desc9102",
            image = "s.gif",
            primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.USABLE,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : GameDatabase() {
            override fun item(name: String): net.sourceforge.kolmafia.data.ItemData? =
                if (name.equals("corpus trendy snack", ignoreCase = true)) item else null
            override fun item(id: Int): net.sourceforge.kolmafia.data.ItemData? = if (id == itemId) item else null
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Trendy"))
        }
        val inv = object : net.sourceforge.kolmafia.inventory.InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                net.sourceforge.kolmafia.inventory.InventoryState(
                    items = mapOf(
                        itemId to net.sourceforge.kolmafia.inventory.InventoryItem(
                            itemId, "corpus trendy snack", 5, net.sourceforge.kolmafia.inventory.ItemType.OTHER,
                        ),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            gameDatabase = db,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_string(available_amount(to_item("corpus trendy snack"))));"""),
        )
    }

    @Test
    fun corpus_haveFamiliar_beecoreBlocksBeeRace() = runBlocking {
        net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase.load()
        val barrrnacle = net.sourceforge.kolmafia.familiar.FamiliarData(
            id = 8, name = "Barn", race = "Barrrnacle",
            weight = 10, experience = 0, kills = 0,
        )
        val fm = net.sourceforge.kolmafia.familiar.FamiliarManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ).also {
            it.testSetState(
                net.sourceforge.kolmafia.familiar.FamiliarState(ownedFamiliars = listOf(barrrnacle)),
            )
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Bees Hate You"))
        }
        val lib = GameRuntimeLibrary(character = char, familiarManager = fm)
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(have_familiar(to_familiar("Barrrnacle"))));"""),
        )
    }

    @Test
    fun corpus_inTerrarium_ownedVsUsable() = runBlocking {
        net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase.load()
        val barrrnacle = net.sourceforge.kolmafia.familiar.FamiliarData(
            id = 8, name = "Barn", race = "Barrrnacle",
            weight = 10, experience = 0, kills = 0,
        )
        val fm = net.sourceforge.kolmafia.familiar.FamiliarManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ).also {
            it.testSetState(
                net.sourceforge.kolmafia.familiar.FamiliarState(ownedFamiliars = listOf(barrrnacle)),
            )
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Bees Hate You"))
        }
        val lib = GameRuntimeLibrary(character = char, familiarManager = fm)
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(in_terrarium(to_familiar("Barrrnacle"))));"""),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(have_familiar(to_familiar("Barrrnacle"))));"""),
        )
    }

    @Test
    fun corpus_retrieveItem_restrictedEarlyExit() = runBlocking {
        val itemId = 9001
        val itemName = "corpus restricted snack"
        val db = object : GameDatabase() {
            override fun item(id: Int) = if (id == itemId) {
                net.sourceforge.kolmafia.data.ItemData(
                    itemId, itemName, "", "", net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                    emptySet(), setOf('t'), 0, null,
                )
            } else null
            override fun item(name: String) = if (name == itemName) item(itemId) else null
        }
        var storageWithdrawn = false
        val storage = object : net.sourceforge.kolmafia.request.StorageRequest(
            HttpClient(MockEngine { respond("") }),
        ) {
            override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
                storageWithdrawn = true
                return Result.success("ok")
            }
        }
        val standard = object : net.sourceforge.kolmafia.request.StandardRequest(
            HttpClient(MockEngine { respond("") }),
        ) {
            override suspend fun ensureInitialized() {
                parseResponse(
                    """
                    <b>Items</b><p><span class="i">$itemName</span><p>
                    """.trimIndent(),
                )
            }
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(hardcore = "1"))
        }
        val inv = object : net.sourceforge.kolmafia.inventory.InventoryManager(
            HttpClient(MockEngine { respond("") }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            override val state = kotlinx.coroutines.flow.MutableStateFlow(
                net.sourceforge.kolmafia.inventory.InventoryState(),
            )
        }
        val retrieve = net.sourceforge.kolmafia.item.RetrieveItemService(
            inventoryManager = inv,
            closetRequest = null,
            storageRequest = storage,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = db,
            character = char,
            standardRequest = standard,
        )
        val lib = GameRuntimeLibrary(
            character = char,
            gameDatabase = db,
            retrieveItemService = retrieve,
        )
        try {
            assertEquals(
                "false",
                outputLib(
                    lib,
                    """print(to_string(retrieve_item(1, to_item("$itemName"))));""",
                ),
            )
            assertFalse(storageWithdrawn)
        } finally {
            net.sourceforge.kolmafia.request.StandardRequest.resetForTest()
        }
    }

    @Test
    fun corpus_useFamiliar_avatarPathBlocked() = runBlocking {
        val goat = net.sourceforge.kolmafia.familiar.FamiliarData(
            id = 7, name = "Biscuit", race = "Angry Goat",
            weight = 12, experience = 0, kills = 0,
        )
        val fm = net.sourceforge.kolmafia.familiar.FamiliarManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ).also {
            it.testSetState(
                net.sourceforge.kolmafia.familiar.FamiliarState(ownedFamiliars = listOf(goat)),
            )
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Avatar of Boris"))
        }
        val lib = GameRuntimeLibrary(character = char, familiarManager = fm)
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(use_familiar(to_familiar("Angry Goat"))));"""),
        )
    }

    @Test
    fun corpus_characterResources_live() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    path = "standard",
                    fury = "3",
                    pp = "8",
                    robonenergy = "42",
                ),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("3", outputLib(lib, """print(to_string(my_fury()));""").trim())
        assertEquals("8", outputLib(lib, """print(to_string(my_pp()));""").trim())
        assertEquals("42", outputLib(lib, """print(to_string(my_robot_energy()));""").trim())
        assertEquals(
            AscensionPath.STANDARD.pathId.toString(),
            outputLib(lib, """print(to_string(my_path_id()));""").trim(),
        )
    }

    @Test
    fun corpus_entityNameAndToInt_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("seal tooth", outputLib(lib, """print(name(to_item("seal tooth")));""").trim())
        assertEquals("1", outputLib(lib, """print(to_string(to_int(to_class("Seal Clubber"))));""").trim())
    }

    @Test
    fun corpus_charpaneClassResources_live() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(path = AscensionPath.DISGUISES_DELIMIT.apiName),
            )
            it.updateClassResource(currentMask = "skull mask", paradoxicity = 11, audience = -5)
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("skull mask", outputLib(lib, """print(my_mask());""").trim())
        assertEquals("11", outputLib(lib, """print(to_string(my_paradoxicity()));""").trim())
        assertEquals("-5", outputLib(lib, """print(to_string(my_audience()));""").trim())
    }

    @Test
    fun corpus_telescopeUpgrades_fromState() {
        val char = KoLCharacter().also { it.setCampground(telescopeUpgrades = 6, telescopeLookedHigh = true) }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("6", outputLib(lib, """print(to_string(telescope_upgrades()));""").trim())
        assertEquals("true", outputLib(lib, """print(to_string(telescope_looked_high()));""").trim())
    }

    @Test
    fun corpus_myGardenType_fromState() {
        val char = KoLCharacter().also { it.setCampground(gardenType = "mushroom") }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("mushroom", outputLib(lib, """print(my_garden_type());""").trim())
    }

    @Test
    fun corpus_myClosetMeat_fromState() {
        val char = KoLCharacter().also { it.setClosetMeat(170_000_000L) }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("170000000", outputLib(lib, """print(my_closet_meat());""").trim())
    }

    @Test
    fun corpus_mySessionMeat_fromState() {
        val char = KoLCharacter().also { it.addSessionMeat(12_345L) }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("12345", outputLib(lib, """print(my_session_meat());""").trim())
    }

    @Test
    fun corpus_descItem_fromCache() = runBlocking {
        DescriptionCache.clear()
        val db = GameDatabase()
        db.load()
        DescriptionCache.cacheItem(
            2,
            """<div id="description"><p>A sharp tooth from a seal.</p><script>""",
        )
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "<p>A sharp tooth from a seal.</p>",
            outputLib(lib, """print(desc(to_item("seal tooth")));""").trim(),
        )
        DescriptionCache.clear()
    }

    @Test
    fun corpus_descItem_prefetchOnMiss() = runBlocking {
        DescriptionCache.clear()
        val db = GameDatabase()
        db.load()
        val engine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("desc_item.php"))
            respond(
                """<div id="description"><p>Prefetched seal tooth.</p><script>""",
                HttpStatusCode.OK,
            )
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            httpClient = HttpClient(engine),
        )
        assertEquals(
            "<p>Prefetched seal tooth.</p>",
            outputLib(lib, """print(desc(to_item("seal tooth")));""").trim(),
        )
        DescriptionCache.clear()
    }

    @Test
    fun corpus_armoryMeatNpc_afterVisitOverlay() {
        registerCorpusItem(99060, "visit-only meat item")
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks(
            """
                <tr rel="99060">
                <a onClick='javascript:descitem(99060)'><b>visit-only meat item</b></a>
                <span title="Meat"><b>250</b></span>
                <form action="shop.php?action=buy&whichshop=armory&whichrow=7777">
                </tr>
            """.trimIndent(),
            "https://www.kingdomofloathing.com/shop.php?whichshop=armory",
        )
        assertEquals("true", outputLib(lib, """print(is_npc_item(99060, true));""").trim())
        net.sourceforge.kolmafia.shop.NpcStoreVisitOverlay.resetForTest()
    }

    @Test
    fun corpus_pullsRemaining_fromStorageHook() {
        ConcoctionDatabase.resetForTest()
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char)
        lib.processVisitResponseHooks(
            """<span class="pullsleft">15</span>""",
            "https://www.kingdomofloathing.com/storage.php?which=5",
        )
        assertEquals("15", outputLib(lib, """print(pulls_remaining());""").trim())
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun corpus_stillsAvailable_fromStillHook() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    ascensions = "2",
                    classId = "5",
                    stills = "0",
                    path = "Avatar of Sneaky Pete",
                ),
            )
        }
        val lib = GameRuntimeLibrary(
            character = char,
            skillManager = corpusSkillManager(StillsAvailability.SUPER_COCKTAIL),
        )
        lib.processVisitResponseHooks(
            """You stand before a still with 3 bright copper stills.""",
            "https://www.kingdomofloathing.com/shop.php?whichshop=still",
        )
        assertEquals("3", outputLib(lib, """print(stills_available());""").trim())
    }

    @Test
    fun corpus_haveMushroomPlot_fromKnollHook() {
        val p = prefs()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(ascensions = "4"))
        }
        val lib = GameRuntimeLibrary(character = char, preferences = p)
        lib.processVisitResponseHooks(
            """<b>Your Mushroom Plot:</b><p><table><tr><td></td></tr></table>""",
            "https://www.kingdomofloathing.com/knoll_mushrooms.php",
        )
        assertEquals("true", outputLib(lib, """print(have_mushroom_plot());""").trim())
    }

    @Test
    fun corpus_craftType_fromConcoctionDatabase() {
        ConcoctionDatabase.resetForTest()
        ConcoctionDatabase.injectForTest(
            net.sourceforge.kolmafia.data.ConcoctionData(
                result = "meat paste",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    net.sourceforge.kolmafia.data.ConcoctionIngredient("meat", 1),
                ),
            ),
        )
        ConcoctionDatabase.injectForTest(
            net.sourceforge.kolmafia.data.ConcoctionData(
                result = "bottle of Definit",
                resultQuantity = 1,
                methods = setOf("STILL", "ROW270"),
                ingredients = listOf(
                    net.sourceforge.kolmafia.data.ConcoctionIngredient("bottle of vodka", 1),
                ),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("Meatpasting", outputLib(lib, """print(craft_type(to_item("meat paste")));""").trim())
        assertEquals(
            "Nash Crosby's Still",
            outputLib(lib, """print(craft_type(to_item("bottle of Definit")));""").trim(),
        )
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun corpus_refreshStatus_updatesCharacterHp() {
        val statusJson = kotlinx.serialization.json.Json.encodeToString(
            CharacterApiResponse.serializer(),
            CharacterApiResponse(hp = "88", hpmax = "100"),
        )
        val client = HttpClient(
            MockEngine {
                respond(
                    statusJson,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(hp = "40", hpmax = "100"))
        }
        val lib = GameRuntimeLibrary(
            character = char,
            characterRequest = CharacterRequest(client),
        )
        assertEquals("true", outputLib(lib, """print(refresh_status());""").trim())
        assertEquals(88, char.state.value.currentHp)
    }

    @Test
    fun corpus_restoreHp_toAmount() = runBlocking {
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
        var apiHp = 25
        val statusJson = {
            kotlinx.serialization.json.Json.encodeToString(
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
                            headersOf(HttpHeaders.ContentType, "application/json"),
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
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(hp = "25", hpmax = "100"))
        }
        val aspirin = InventoryItem(1381, "aspirin", 2, ItemType.OTHER)
        val inv = object : InventoryManager(client, bus) {
            init {
                _state.value = InventoryState(items = mapOf(1381 to aspirin))
            }
        }
        val rm = RecoveryManager(
            inv,
            SkillManager(client, SkillCastRequest(client), bus),
            prefs(),
        )
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
    fun corpus_moodExecute_and_zodiacSigns_live() {
        val cast = mutableListOf<Int>()
        val client = HttpClient(MockEngine { respond("") })
        val skills = object : SkillManager(client, SkillCastRequest(client), GameEventBus()) {
            init {
                learnLocalSkill(
                    SkillData(200, "Skill 200", SkillType.PASSIVE, mpCost = 10, dailyLimit = 0, timesCast = 0),
                )
            }

            override suspend fun cast(skill: SkillData, quantity: Int): Result<Unit> {
                repeat(quantity) { cast.add(skill.id) }
                return Result.success(Unit)
            }
        }
        val settings = com.russhwolf.settings.MapSettings()
        settings.putBoolean(net.sourceforge.kolmafia.preferences.Preferences.AUTO_BUFF, true)
        val moodPrefs = net.sourceforge.kolmafia.preferences.Preferences(settings)
        val mood = net.sourceforge.kolmafia.mood.MoodManager(skills, moodPrefs)
        mood.activeMood = net.sourceforge.kolmafia.mood.Mood(
            "test",
            listOf(net.sourceforge.kolmafia.mood.MoodTrigger(10, "Effect 10", 200, "Skill 200", 1)),
        )
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(sign = "Mongoose", mp = "50", mpmax = "100"),
            )
        }
        val lib = GameRuntimeLibrary(
            character = char,
            moodManager = mood,
            skillManager = skills,
        )
        assertEquals("true", outputLib(lib, """print(in_muscle_sign());""").trim())
        assertEquals("false", outputLib(lib, """print(in_bad_moon());""").trim())
        outputLib(lib, """mood_execute(0);""")
        assertEquals(listOf(200), cast)
    }

    @Test
    fun corpus_consumptionLimits_live() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    path = "Standard",
                    sign = "Mongoose",
                    stomachsize = "15",
                    liversize = "14",
                    spleensize = "15",
                ),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("true", outputLib(lib, """print(can_eat());""").trim())
        assertEquals("true", outputLib(lib, """print(can_drink());""").trim())
        assertEquals("15", outputLib(lib, """print(fullness_limit());""").trim())
        assertEquals("14", outputLib(lib, """print(inebriety_limit());""").trim())
        assertEquals("15", outputLib(lib, """print(spleen_limit());""").trim())
    }

    @Test
    fun corpus_itemProperties_live() {
        ItemDatabase.resetForTest()
        ItemDatabase.registerForTest(
            ItemData(
                id = 6001,
                name = "corpus trade item",
                descId = "d6001",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'g', 'd'),
                autosellPrice = 10,
                plural = "corpus trade items",
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(is_tradeable(to_item("corpus trade item")));""").trim())
        assertEquals("corpus trade items", outputLib(lib, """print(to_plural(to_item("corpus trade item")));""").trim())
    }

    @Test
    fun corpus_craftIntrospection_live() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        ItemDatabase.registerForTest(
            ItemData(
                id = 6101,
                name = "corpus brew",
                descId = "d6101",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 6102,
                name = "corpus malt",
                descId = "d6102",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus malt",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus brew",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("corpus malt", 2)),
            ),
        )
        val inventory = object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(6102 to InventoryItem(6102, "corpus malt", 6, ItemType.OTHER)),
                ),
            )
            override val state = flow.asStateFlow()
        }
        val lib = GameRuntimeLibrary(inventoryManager = inventory)
        ConcoctionDatabase.refreshConcoctionsFromInventory(mapOf(6102 to 6))
        assertEquals("3", outputLib(lib, """print(creatable_amount(to_item("corpus brew")));""").trim())
        assertEquals("1", outputLib(lib, """print(count(get_ingredients(to_item("corpus brew"))));""").trim())
    }

    @Test
    fun corpus_shopProbes_live() {
        ItemDatabase.resetForTest()
        NpcStoreDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
        ItemDatabase.registerForTest(
            ItemData(
                id = 6201,
                name = "corpus npc widget",
                descId = "d6201",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 6202,
                name = "corpus coin widget",
                descId = "d6202",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        NpcStoreDatabase.loadFromText("Corpus Shop\tcorp\tcorpus npc widget\t50\n")
        CoinmasterDatabase.loadFromText(
            shopsText = "corpcoin\tCorpus Coin\n",
            coinText = "Corpus Coin\tbuy\t100\tcorpus coin widget\tROW6202\n",
        )
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(is_npc_item(to_item("corpus npc widget")));""").trim())
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(to_item("corpus coin widget")));""").trim())
    }

    @Test
    fun corpus_craftDepth_live() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        registerCorpusItem(6301, "corpus smithable")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus smithable",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("0", outputLib(lib, """print(creatable_turns(to_item("corpus smithable")));""").trim())
    }

    @Test
    fun corpus_creatableTurnsDepth_live() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        registerCorpusItem(338, "tenderizing hammer")
        registerCorpusItem(6310, "corpus layered product")
        registerCorpusItem(6311, "corpus smith part")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus layered product",
                resultQuantity = 1,
                methods = setOf("SMITH", "HAMMER"),
                ingredients = listOf(ConcoctionIngredient("corpus smith part", 1)),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus smith part",
                resultQuantity = 1,
                methods = setOf("SMITH", "HAMMER"),
                ingredients = emptyList(),
            ),
        )
        val inventory = object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        338 to InventoryItem(338, "tenderizing hammer", 1, ItemType.OTHER),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }
        val lib = GameRuntimeLibrary(inventoryManager = inventory)
        assertEquals("2", outputLib(lib, """print(creatable_turns(to_item("corpus layered product")));""").trim())
    }

    @Test
    fun corpus_freeCrafts_live() {
        val effectsJson = """{"716":{"name":"Inigo's Incantation of Inspiration","duration":10}}"""
        val engine = MockEngine { request ->
            when (request.url.parameters["what"]) {
                "effects" -> respond(
                    content = effectsJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> respond("ok", HttpStatusCode.OK)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val effectManager = EffectManager(client, GameEventBus())
        runBlocking { effectManager.fetchEffects() }
        val lib = GameRuntimeLibrary(effectManager = effectManager)
        assertEquals("2", outputLib(lib, """print(free_crafts());""").trim())
    }

    @Test
    fun corpus_shopValidate_live() {
        registerCorpusItem(9801, "corpus validate npc")
        NpcStoreDatabase.loadFromText("Corpus Shop\tcorpstore\tcorpus validate npc\t25\n")
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(npc_item_accessible(9801));""").trim())
    }

    @Test
    fun corpus_canExpand_live() {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(can_expand_stomach());""").trim())
        assertEquals("true", outputLib(lib, """print(can_expand_liver());""").trim())
    }

    @Test
    fun corpus_vykeaConcoctionPrice_live() {
        registerCorpusItem(8729, "VYKEA hex key")
        registerCorpusItem(8730, "VYKEA instructions")
        registerCorpusItem(8725, "VYKEA plank")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "level 1 couch",
                resultQuantity = 1,
                methods = setOf("VYKEA"),
                ingredients = listOf(
                    ConcoctionIngredient("VYKEA instructions", 1),
                    ConcoctionIngredient("VYKEA plank", 10),
                ),
            ),
        )
        val mall = object : net.sourceforge.kolmafia.mall.MallManager(
            net.sourceforge.kolmafia.mall.MallSearchRequest(
                HttpClient(MockEngine { respond("[]") }),
            ),
            net.sourceforge.kolmafia.mall.MallPurchaseRequest(
                HttpClient(MockEngine { respond("") }),
            ),
            null,
        ) {
            override suspend fun cheapestPrice(itemName: String): Long = when {
                itemName.equals("VYKEA instructions", ignoreCase = true) -> 111L
                itemName.equals("VYKEA plank", ignoreCase = true) -> 5L
                else -> -1L
            }
        }
        val lib = GameRuntimeLibrary(mallManager = mall)
        assertEquals(
            "161",
            outputLib(lib, """print(concoction_price(to_vykea("level 1 couch")));""").trim(),
        )
    }

    @Test
    fun corpus_floundryCraftGate_live() {
        registerCorpusItem(9001, "carpe")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "carpe",
                resultQuantity = 1,
                methods = setOf("FLOUNDRY"),
                ingredients = emptyList(),
            ),
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9001));""").trim())
        prefs.setBoolean(net.sourceforge.kolmafia.clan.ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true)
        net.sourceforge.kolmafia.data.FloundryAvailability.addForTest("carpe", 100)
        ConcoctionDatabase.refreshConcoctionsNow(
            net.sourceforge.kolmafia.data.ConcoctionRefreshContext(
                preferences = prefs,
            ),
        )
        assertEquals("true", outputLib(lib, """print(is_craft_permitted(9001));""").trim())
    }

    @Test
    fun corpus_terminalCraftGate_live() {
        registerCorpusItem(9033, "Source terminal")
        registerCorpusItem(9902, "browser cookie")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "browser cookie",
                resultQuantity = 1,
                methods = setOf("TERMINAL"),
                ingredients = emptyList(),
            ),
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9902));""").trim())
        prefs.setInt("_sourceTerminalExtrudes", 0)
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9902));""").trim())
    }

    @Test
    fun corpus_generalStoreValidate_live() {
        registerCorpusItem(3128, "marshmallow")
        net.sourceforge.kolmafia.data.NpcStoreDatabase.loadFromText(
            "General Store\tgeneralstore\tmarshmallow\t10\n",
        )
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(is_npc_item(3128, true));""").trim())
    }

    @Test
    fun corpus_gnomePartCraftGate_live() {
        registerCorpusItem(9903, "gnomish athlete's foot")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "gnomish athlete's foot",
                resultQuantity = 1,
                methods = setOf("GNOME_PART"),
                ingredients = emptyList(),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9903));""").trim())
    }

    @Test
    fun corpus_giftShopValidate_live() {
        registerCorpusItem(1180, "potted fern")
        net.sourceforge.kolmafia.data.NpcStoreDatabase.loadFromText(
            "Gift Shop\ttown_giftshop.php\tpotted fern\t200\n",
        )
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(is_npc_item(1180, true));""").trim())
    }

    @Test
    fun corpus_cookWithoutOvenGate_live() {
        registerCorpusItem(9904, "corpus hot dish")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus hot dish",
                resultQuantity = 1,
                methods = setOf("COOK"),
                ingredients = emptyList(),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9904));""").trim())
    }

    @Test
    fun corpus_bartenderValidate_live() {
        registerCorpusItem(9405, "plain old beer")
        net.sourceforge.kolmafia.data.NpcStoreDatabase.loadFromText(
            "The Typical Tavern\tbartender\tplain old beer\t50\n",
        )
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(is_npc_item(9405, true));""").trim())
    }

    @Test
    fun corpus_staffGate_live() {
        registerCorpusItem(9905, "corpus chefstaff")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus chefstaff",
                resultQuantity = 1,
                methods = setOf("STAFF"),
                ingredients = emptyList(),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9905));""").trim())
    }

    @Test
    fun corpus_isCoinmasterItemValidate_live() {
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(7185, true));""").trim())
    }

    @Test
    fun corpus_cookFancyWillBuyTool_live() {
        val prefs = prefs()
        prefs.setBoolean("autoSatisfyWithNPCs", true)
        val char = net.sourceforge.kolmafia.character.KoLCharacter().also {
            it.updateMeat(2000)
            it.updateAdventuresLeft(3)
        }
        registerCorpusItem(9912, "corpus auto fancy")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus auto fancy",
                resultQuantity = 1,
                methods = setOf("COOK_FANCY"),
                ingredients = emptyList(),
            ),
        )
        val lib = GameRuntimeLibrary(preferences = prefs, character = char)
        assertEquals("true", outputLib(lib, """print(is_craft_permitted(9912));""").trim())
    }

    @Test
    fun corpus_isNpcItemOneArg_live() {
        registerCorpusItem(9913, "corpus one arg npc")
        net.sourceforge.kolmafia.data.NpcStoreDatabase.loadFromText(
            "General Store\tgeneralstore\tcorpus one arg npc\t50\n",
        )
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(is_npc_item(9913));""").trim())
    }

    @Test
    fun corpus_smithKnollGate_live() {
        registerCorpusItem(9914, "corpus smith knoll gate")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus smith knoll gate",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "test",
                    level = "5",
                    classId = "1",
                    sign = "Vole",
                ),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("true", outputLib(lib, """print(is_craft_permitted(9914));""").trim())
    }

    @Test
    fun corpus_replicaStoreYearGate_live() {
        val p = prefs()
        p.setInt("currentReplicaStoreYear", 2023)
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11190, true));""").trim())
    }

    @Test
    fun corpus_tikiCraftGate_live() {
        registerCorpusItem(9915, "corpus tiki mix")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus tiki mix",
                resultQuantity = 1,
                methods = setOf("ROLLING_PIN", "TIKI"),
                ingredients = emptyList(),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9915));""").trim())
        val libWithSkill = GameRuntimeLibrary(
            skillManager = corpusSkillManager(186),
        )
        assertEquals("true", outputLib(libWithSkill, """print(is_craft_permitted(9915));""").trim())
    }

    @Test
    fun corpus_rollingPinAlwaysPermitted_live() {
        registerCorpusItem(9916, "corpus rolling pin item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus rolling pin item",
                resultQuantity = 1,
                methods = setOf("ROLLING_PIN"),
                ingredients = emptyList(),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(is_craft_permitted(9916));""").trim())
    }

    @Test
    fun corpus_starchartTorsoGate_live() {
        ItemDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
        val p = prefs()
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        registerCorpusItem(1133, "star shirt")
        CoinmasterDatabase.loadFromText(
            shopsText = "starchart\tA Star Chart\n",
            coinText = "A Star Chart\tbuy\t1\tstar shirt\tROW1133\n",
        )
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "test",
                    level = "10",
                    classId = "1",
                    sign = "Vole",
                    meat = "100",
                ),
            )
        }
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(1133, true));""").trim())
        val libWithTorso = GameRuntimeLibrary(
            preferences = p,
            character = char,
            skillManager = corpusSkillManager(12),
        )
        assertEquals("true", outputLib(libWithTorso, """print(is_coinmaster_item(1133, true));""").trim())
    }

    @Test
    fun corpus_wsmithCraftGate_live() {
        registerCorpusItem(9917, "corpus wsmith crossbow")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus wsmith crossbow",
                resultQuantity = 1,
                methods = setOf("WSMITH"),
                ingredients = emptyList(),
            ),
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            skillManager = corpusSkillManager(1006),
        )
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9917));""").trim())
        val libWithHammer = GameRuntimeLibrary(
            preferences = prefs,
            skillManager = corpusSkillManager(1006),
            inventoryManager = object : net.sourceforge.kolmafia.inventory.InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    net.sourceforge.kolmafia.inventory.InventoryState(
                        items = mapOf(
                            338 to net.sourceforge.kolmafia.inventory.InventoryItem(
                                itemId = 338,
                                name = "tenderizing hammer",
                                quantity = 1,
                                type = net.sourceforge.kolmafia.inventory.ItemType.OTHER,
                            ),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("true", outputLib(libWithHammer, """print(is_craft_permitted(9917));""").trim())
    }

    @Test
    fun corpus_tinkerCraftGate_live() {
        registerCorpusItem(9918, "corpus clockwork widget")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "corpus clockwork widget",
                resultQuantity = 1,
                methods = setOf("TINKER"),
                ingredients = emptyList(),
            ),
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        val char = KoLCharacter().apply {
            updateFromApiResponse(
                CharacterApiResponse(
                    sign = "Wombat",
                    ascensions = "1",
                ),
            )
        }
        val lib = GameRuntimeLibrary(preferences = prefs, character = char)
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9918));""").trim())
        prefs.setInt("lastDesertUnlock", 1)
        assertEquals("true", outputLib(lib, """print(is_craft_permitted(9918));""").trim())
    }

    @Test
    fun corpus_baconCoinmasterValidate_live() {
        registerCorpusItem(9017, "viral video")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "bacon\tInternet Meme Shop\n",
            coinText = "Internet Meme Shop\tROW9017\tviral video\tBACON (20)\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setBoolean("_internetViralVideoBought", true)
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(9017, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_fixodentCoinmasterValidate_live() {
        registerCorpusItem(11977, "dentadent")
        registerCorpusItem(11975, "Monodent of the Sea")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "fixodent\tCraft with Teeth\n",
            coinText = "Craft with Teeth\tROW11977\tdentadent\tfixodent\tloose teeth\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11977, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_wildfireNpcValidate_live() {
        registerCorpusItem(10790, "B. L. A. R. T.")
        net.sourceforge.kolmafia.data.NpcStoreDatabase.loadFromText(
            "FDKOL Auxiliary\twildfire\tB. L. A. R. T.\t10000\tROW1258\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("itemBoughtPerAscension10790", true)
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                net.sourceforge.kolmafia.character.CharacterApiResponse(
                    path = net.sourceforge.kolmafia.character.AscensionPath.WILDFIRE.apiName,
                ),
            )
        }
        val lib = GameRuntimeLibrary(preferences = prefs, character = char)
        assertEquals("false", outputLib(lib, """print(is_npc_item(10790, true));""").trim())
        net.sourceforge.kolmafia.data.NpcStoreDatabase.resetForTest()
    }

    @Test
    fun corpus_mysticPsychosisPixelValidate_live() {
        registerCorpusItem(5906, "pixel pill")
        registerCorpusItem(461, "red pixel")
        registerCorpusItem(459, "white pixel")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "mystic\tThe Crackpot Mystic's Shed\n",
            coinText = "The Crackpot Mystic's Shed\tROW39\tpixel pill\tred pixel (20)\twhite pixel (20)\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(level = "6"))
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(5906, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<tr rel="5906"><b>pixel pill</b></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mystic",
        )
        val libWithPixels = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(level = "6"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            461 to InventoryItem(461, "red pixel", 20, ItemType.OTHER),
                            459 to InventoryItem(459, "white pixel", 20, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("true", outputLib(libWithPixels, """print(is_coinmaster_item(5906, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_hippyPeachNpcValidate_live() {
        registerCorpusItem(673, "peach")
        net.sourceforge.kolmafia.data.NpcStoreDatabase.loadFromText(
            "Hippy Store (Hippy)\thippy\tpeach\t300\tROW673\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithNPCs", true)
        prefs.setString(net.sourceforge.kolmafia.quest.Quest.ISLAND_WAR.prefKey, "step2")
        net.sourceforge.kolmafia.shop.NpcShopSync.syncFromStoreHtml(
            storeKey = "hippy",
            html = "peach pear plum",
            prefs = prefs,
            ascensionNumber = 1,
        )
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(ascensions = "1"))
            },
        )
        assertEquals("true", outputLib(lib, """print(is_npc_item(673, true));""").trim())
        net.sourceforge.kolmafia.data.NpcStoreDatabase.resetForTest()
    }

    @Test
    fun corpus_shoreToasterCoinmasterValidate_live() {
        registerCorpusItem(637, "cheap toaster")
        registerCorpusItem(338, "Shore Inc. Ship Trip Scrip")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "shore\tThe Shore, Inc. Gift Shop\n",
            coinText = "The Shore, Inc. Gift Shop\tROW637\tcheap toaster\tShore Inc. Ship Trip Scrip (20)\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setInt("lastDesertUnlock", 1)
        prefs.setBoolean("itemBoughtPerAscension637", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(level = "6", ascensions = "1"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            338 to InventoryItem(338, "Shore Inc. Ship Trip Scrip", 20, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(637, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<b>cheap toaster</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=shore",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(637, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_fwshopHatNpcValidate_live() {
        registerCorpusItem(10762, "fedora-mounted fountain")
        net.sourceforge.kolmafia.data.NpcStoreDatabase.loadFromText(
            "Clan Underground Fireworks Shop\tfwshop\tfedora-mounted fountain\t500\tROW1247\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithNPCs", true)
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("false", outputLib(lib, """print(is_npc_item(10762, true));""").trim())
        net.sourceforge.kolmafia.shop.NpcShopSync.syncFromStoreHtml(
            storeKey = "fwshop",
            html = """<b>Combat Explosives</b><b>Dangerous Hats</b>""",
            prefs = prefs,
            ascensionNumber = 1,
        )
        assertEquals("true", outputLib(lib, """print(is_npc_item(10762, true));""").trim())
        net.sourceforge.kolmafia.data.NpcStoreDatabase.resetForTest()
    }

    @Test
    fun corpus_hiddenTavernNpcValidate_live() {
        registerCorpusItem(175, "Fog Murderer")
        net.sourceforge.kolmafia.data.NpcStoreDatabase.loadFromText(
            "The Hidden Tavern\thiddentavern\tFog Murderer\t500\tROW175\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithNPCs", true)
        val char = KoLCharacter().apply {
            updateFromApiResponse(CharacterApiResponse(ascensions = "2"))
        }
        val lib = GameRuntimeLibrary(preferences = prefs, character = char)
        assertEquals("false", outputLib(lib, """print(is_npc_item(175, true));""").trim())
        net.sourceforge.kolmafia.shop.NpcShopSync.syncFromStoreHtml(
            storeKey = "hiddentavern",
            html = "<html>Hidden Tavern</html>",
            prefs = prefs,
            ascensionNumber = 2,
        )
        assertEquals("true", outputLib(lib, """print(is_npc_item(175, true));""").trim())
        net.sourceforge.kolmafia.data.NpcStoreDatabase.resetForTest()
    }

    @Test
    fun corpus_swaggerCoinmasterValidate_live() {
        registerCorpusItem(7732, "Black Bart's Booty")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "swagger\tThe Swagger Shop\n",
            coinText = "The Swagger Shop\tbuy\t1000\tBlack Bart's Booty\tROW7732\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(7732, true));""").trim())
        lib.processVisitResponseHooks(
            html = """
                You've earned 1200 swagger during a pirate season.
                <tr><td><b>Black Bart's Booty</b></td>
                <td><form><input type="hidden" name="whichitem" value="7732" />
                <input type="submit" value="Buy (1000 swagger)" /></form></td></tr>
            """.trimIndent(),
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(7732, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_jarlCosmicSixPackCoinmasterValidate_live() {
        registerCorpusItem(6237, "cosmic six-pack")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "jarl\tJarlsberg's Cosmic Kitchen\n",
            coinText = "Jarlsberg's Cosmic Kitchen\tbuy\t1\tcosmic six-pack\tROW6237\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val char = KoLCharacter().apply {
            updateFromApiResponse(
                CharacterApiResponse(
                    path = net.sourceforge.kolmafia.character.AscensionPath.AVATAR_OF_JARLSBERG.apiName,
                    meat = "100",
                ),
            )
        }
        val lib = GameRuntimeLibrary(preferences = prefs, character = char)
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(6237, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterPurchasePrefs.applyPurchasedItem(
            master = net.sourceforge.kolmafia.shop.CoinmasterData(
                masterName = "Jarlsberg's Cosmic Kitchen",
                nickname = "jarl",
                token = null,
                shopId = "jarl",
                buyItems = emptyList(),
                sellItems = emptyList(),
            ),
            itemId = 6237,
            prefs = prefs,
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(6237, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_replicaMrStoreCoinmasterValidate_live() {
        registerCorpusItem(11190, "replica Dark Jill-O-Lantern")
        registerCorpusItem(11325, "august scepter")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "mrreplica\tReplica Mr. Store\n",
            coinText = """
                Replica Mr. Store	buy	1	replica Dark Jill-O-Lantern	ROW11190
                Replica Mr. Store	buy	1	august scepter	ROW11325
            """.trimIndent(),
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(
                    CharacterApiResponse(
                        path = net.sourceforge.kolmafia.character.AscensionPath.LEGACY_OF_LOATHING.apiName,
                        meat = "100000",
                    ),
                )
            },
        )
        lib.processVisitResponseHooks(
            html = """<td colspan=14 align=center>&mdash; <b>2023</b> &mdash;</td>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mrreplica",
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11190, true));""").trim())
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11325, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_blackMarketCoinmasterValidate_live() {
        registerCorpusItem(7185, "Red Zeppelin ticket")
        registerCorpusItem(7221, "priceless diamond")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "blackmarket\tThe Black Market\n",
            coinText = "The Black Market\tROW290\tRed Zeppelin ticket\tpriceless diamond (1)\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(ascensions = "3", meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            7221 to InventoryItem(7221, "priceless diamond", 5, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(7185, true));""").trim())
        lib.processVisitResponseHooks(
            html = "<html>The Black Market</html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=blackmarket",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(7185, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_pirateRealmFunALogValidate_live() {
        registerCorpusItem(10199, "crabsicle")
        registerCorpusItem(10225, "PirateRealm fun-a-log")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "piraterealm\tPirateRealm Fun-a-Log\n",
            coinText = "PirateRealm Fun-a-Log\tbuy\t100\tcrabsicle\tROW1053\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            10225 to InventoryItem(10225, "PirateRealm fun-a-log", 1, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(10199, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<tr rel="10199"><td>crabsicle</td></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(10199, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_dripArmoryShieldValidate_live() {
        registerCorpusItem(DripArmoryPrefs.DRIPPY_SHIELD, "drippy shield")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "driparmory\tDrip Institute Armory\n",
            coinText = "Drip Institute Armory\tbuy\t50\tdrippy shield\tROW1132\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
        )
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item(${DripArmoryPrefs.DRIPPY_SHIELD}, true));""").trim(),
        )
        lib.processVisitResponseHooks(
            html = """<b>drippy shield</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory",
        )
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item(${DripArmoryPrefs.DRIPPY_SHIELD}, true));""").trim(),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_alliedHqFlakShieldValidate_live() {
        registerCorpusItem(11920, "flak shield")
        registerCorpusItem(7567, "Chroner")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "twitch_alliedhq\tAllied HQ\n",
            coinText = "Allied HQ\tROW1599\tflak shield\tChroner (20)\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            7567 to InventoryItem(7567, "Chroner", 100, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11920, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<b>flak shield</b> Chroner (20)""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=twitch_alliedhq",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11920, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_broberryBrogurtValidate_live() {
        registerCorpusItem(7455, "broberry brogurt")
        registerCorpusItem(7429, "Beach Buck")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "sbb_brogurt\tThe Frozen Brogurt Stand\n",
            coinText = "The Frozen Brogurt Stand\tbuy\t10\tbroberry brogurt\tROW295\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(7455, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<center><b>Adventure Results</b></center>""",
            url = "https://www.kingdomofloathing.com/adventure.php?snarfblat=402",
        )
        prefs.setString("questESlBacteria", "finished")
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(7455, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_flowerTradeinValidate_live() {
        registerCorpusItem(7567, "Chroner")
        registerCorpusItem(8668, "rose")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "flowertradein\tThe Central Loathing Floral Mercantile Exchange\n",
            coinText = "The Central Loathing Floral Mercantile Exchange\tbuy\t1\tChroner\tROW759\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            8668 to InventoryItem(8668, "rose", 1, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(7567, true));""").trim())
        net.sourceforge.kolmafia.shop.FlowerTradeinSync.syncFromShopHtml(
            """
                <tr rel="7567">
                <a onClick='javascript:descitem(7567)'><b>Chroner</b></a>
                <span title="rose"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
                </tr>
            """.trimIndent(),
            prefs,
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(7567, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_conmerchTattooValidate_live() {
        registerCorpusItem(9148, "Twitching Television Tattoo")
        registerCorpusItem(7567, "Chroner")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = "KoL Con 13 Merch Table\tbuy\t1\tTwitching Television Tattoo\tROW895\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            7567 to InventoryItem(7567, "Chroner", 2000, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(9148, true));""").trim())
        prefs.setBoolean(net.sourceforge.kolmafia.shop.TimeTowerSync.PREF, true)
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(9148, true));""").trim())
        net.sourceforge.kolmafia.shop.MerchTableSync.syncFromShopHtml(
            """
                <tr rel="9148">
                <a onClick='javascript:descitem(9148)'><b>Twitching Television Tattoo</b></a>
                <span title="Chroner"><b>1111</b></span>
                <form action="shop.php?action=buy&whichshop=conmerch&whichrow=895">
                </tr>
            """.trimIndent(),
            prefs,
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(9148, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_conmerchVisitOverlayValidate_live() {
        registerCorpusItem(9148, "Twitching Television Tattoo")
        registerCorpusItem(7567, "Chroner")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = """
                KoL Con 13 Merch Table	buy	1	Twitching Television Tattoo	ROW895
            """.trimIndent(),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            7567 to InventoryItem(7567, "Chroner", 2000, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(9148, true));""").trim())
        prefs.setBoolean(net.sourceforge.kolmafia.shop.TimeTowerSync.PREF, true)
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(9148, true));""").trim())
        lib.processVisitResponseHooks(
            """
                <tr rel="9148">
                <a onClick='javascript:descitem(9148)'><b>Twitching Television Tattoo</b></a>
                <span title="Chroner"><b>1111</b></span>
                <form action="shop.php?action=buy&whichshop=conmerch&whichrow=895">
                </tr>
            """.trimIndent(),
            "shop.php?whichshop=conmerch",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(9148, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.replaceBuyRows(
            net.sourceforge.kolmafia.shop.MerchTableSync.SHOP_ID,
            emptyList(),
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(9148, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun corpus_crimbo25SammyValidate_live() {
        registerCorpusItem(12121, "Crymbocurrency")
        registerCorpusItem(1452, "cold wad")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "crimbo25_sammy\tThe HMS Bounty Hunter\n",
            coinText = "The HMS Bounty Hunter\tROW1649\tCrymbocurrency (5)\tcold wad\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            1452 to InventoryItem(1452, "cold wad", 5, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(12121, true));""").trim())
        net.sourceforge.kolmafia.shop.Crimbo25SammySync.syncFromShopHtml(
            """
                <tr rel="12121">
                <a onClick='javascript:descitem(12121)'><b>Crymbocurrency (5)</b></a>
                <span title="cold wad"><b>2</b></span>
                <form action="shop.php?action=buy&whichshop=crimbo25_sammy&whichrow=1649">
                </tr>
            """.trimIndent(),
            prefs,
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(12121, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_crimbo23ArmoryValidate_live() {
        registerCorpusItem(11440, "Kelflar vest")
        registerCorpusItem(11407, "Crimbuccaneer shirt")
        registerCorpusItem(11402, "Elf Army machine parts")
        registerCorpusItem(11405, "Crimbuccaneer flotsam")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = """
                crimbo23_elf_armory	Elf Guard Armory
                crimbo23_pirate_armory	Crimbuccaneer Junkworks
            """.trimIndent(),
            coinText = """
                Elf Guard Armory	ROW1415	Kelflar vest	Elf Army machine parts (3)
                Crimbuccaneer Junkworks	ROW1418	Crimbuccaneer shirt	Crimbuccaneer flotsam (3)
            """.trimIndent(),
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            11402 to InventoryItem(11402, "Elf Army machine parts", 10, ItemType.OTHER),
                            11405 to InventoryItem(11405, "Crimbuccaneer flotsam", 10, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11440, true));""").trim())
        prefs.setString("crimbo23ArmoryControl", "elf")
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11440, true));""").trim())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11407, true));""").trim())
        prefs.setString("crimbo23ArmoryControl", "pirate")
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11440, true));""").trim())
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11407, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_merchTablePrefValidate_live() {
        registerCorpusItem(9148, "Twitching Television Tattoo")
        registerCorpusItem(7567, "Chroner")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = "KoL Con 13 Merch Table\tbuy\t1\tTwitching Television Tattoo\tROW895\n",
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setBoolean(net.sourceforge.kolmafia.shop.TimeTowerSync.PREF, true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = emptyInventoryManager(),
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(9148, true));""").trim())
        net.sourceforge.kolmafia.shop.MerchTableSync.syncFromShopHtml(
            """
                You have 2,000 Mr. Chroner to trade.
                <tr rel="9148">
                <a onClick='javascript:descitem(9148)'><b>Twitching Television Tattoo</b></a>
                <span title="Chroner"><b>1111</b></span>
                <form action="shop.php?action=buy&whichshop=conmerch&whichrow=895">
                </tr>
            """.trimIndent(),
            prefs,
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(9148, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_crimbo23BarCafeValidate_live() {
        registerCorpusItem(11465, "mulled wine")
        registerCorpusItem(11459, "sugarplum ration")
        registerCorpusItem(11408, "Elf Guard MPC")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = """
                crimbo23_elf_bar	Elf Guard Officers' Club
                crimbo23_elf_cafe	Elf Guard Mess Hall
            """.trimIndent(),
            coinText = """
                Elf Guard Officers' Club	ROW1406	mulled wine	Elf Guard MPC (5)
                Elf Guard Mess Hall	ROW1400	sugarplum ration	Elf Guard MPC (5)
            """.trimIndent(),
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setInt(net.sourceforge.kolmafia.shop.Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = emptyInventoryManager(),
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11465, true));""").trim())
        prefs.setString("crimbo23BarControl", "elf")
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11465, true));""").trim())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11459, true));""").trim())
        prefs.setString("crimbo23CafeControl", "elf")
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11459, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_crimbo23FactoryValidate_live() {
        registerCorpusItem(11480, "trick coin")
        registerCorpusItem(11487, "prank Crimbo card")
        registerCorpusItem(11408, "Elf Guard MPC")
        registerCorpusItem(11409, "Crimbuccaneer piece of 12")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = """
                crimbo23_elf_factory	Elf Guard Toy and Munitions Factory
                crimbo23_pirate_factory	Crimbuccaneer Foundry
            """.trimIndent(),
            coinText = """
                Elf Guard Toy and Munitions Factory	ROW1424	trick coin	Elf Guard MPC (10)
                Crimbuccaneer Foundry	ROW1431	prank Crimbo card	Crimbuccaneer piece of 12 (10)
            """.trimIndent(),
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setInt(net.sourceforge.kolmafia.shop.Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)
        prefs.setInt(net.sourceforge.kolmafia.shop.Crimbo23ShopSync.AVAILABLE_PIECE_OF_12_PREF, 20)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = emptyInventoryManager(),
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11480, true));""").trim())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11487, true));""").trim())
        prefs.setString("crimbo23FoundryControl", "elf")
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11480, true));""").trim())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11487, true));""").trim())
        prefs.setString("crimbo23FoundryControl", "pirate")
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11480, true));""").trim())
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11487, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_armoryStandardRewardValidate_live() {
        registerCorpusItem(11504, "moss mace")
        registerCorpusItem(11510, "moss mulch")
        net.sourceforge.kolmafia.data.StandardRewardDatabase.loadFromText(
            """
                11504	2024	norm	SC	ROW1454	moss mace
            """.trimIndent(),
            """
                11510	2024	norm	moss mulch
            """.trimIndent(),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "armory\tArmory and Leggery\n",
            coinText = "",
        )
        net.sourceforge.kolmafia.shop.ArmoryAndLeggeryShopRows.rebuild()
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                    respond("ok", io.ktor.http.HttpStatusCode.OK)
                }),
                net.sourceforge.kolmafia.event.GameEventBus(),
            ) {
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            11510 to InventoryItem(11510, "moss mulch", 2, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11504, true));""").trim())
        net.sourceforge.kolmafia.data.StandardRewardDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_derivedPulverize_potteryHat() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "666666",
            outputLib(
                lib,
                """print(pulverize(to_item("pottery hat"))[to_item("hot powder")]);""",
            ).trim(),
        )
    }

    @Test
    fun corpus_shopRowDatabase_loadsBundledRow() {
        registerCorpusItem(99001, "FDKOL tattoo")
        registerCorpusItem(99002, "FDKOL commendation")
        net.sourceforge.kolmafia.shop.ShopRowDatabase.loadFromText(
            shopRowsText = "3\tfdkol\tFDKOL tattoo\tFDKOL commendation (100)\n",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val row = net.sourceforge.kolmafia.shop.ShopRowDatabase.getShopRow(3)
        assertEquals(99001, row?.item?.itemId)
        assertEquals(99002, row?.costs?.single()?.itemId)
        assertEquals(100, row?.costs?.single()?.count)
        net.sourceforge.kolmafia.shop.ShopRowDatabase.resetForTest()
    }

    @Test
    fun corpus_shopVisitLearn_sessionLog() {
        registerCorpusItem(99201, "visit-learned item")
        registerCorpusItem(99202, "FDKOL commendation")
        val prefs = Preferences(MapSettings())
        val sessionLogger = net.sourceforge.kolmafia.session.SessionLogger(prefs, GameEventBus())
        net.sourceforge.kolmafia.shop.ShopInventorySync.parseAndLearn(
            html = """
                <tr rel="99201">
                <a onClick='javascript:descitem(99201)'><b>visit-learned item</b></a>
                <span title="FDKOL commendation"><b>75</b></span>
                <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1500">
                </tr>
            """.trimIndent(),
            url = "shop.php?whichshop=fdkol",
            sessionLogger = sessionLogger,
        )
        val log = prefs.getString(net.sourceforge.kolmafia.session.SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("1500\tfdkol\tvisit-learned item\tFDKOL commendation (75)"))
        net.sourceforge.kolmafia.shop.ShopRowDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun corpus_visitLearnedCoinmasterValidate_live() {
        registerCorpusItem(99301, "visit-learned item")
        registerCorpusItem(99302, "FDKOL commendation")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "fdkol\tFDKOL Requisitions Tent\tNPCCOIN\n",
            coinText = "FDKOL Requisitions Tent\tbuy\t75\tvisit-learned item\tROW1500\n",
        )
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.registerVisitBuyRows(
            "fdkol",
            listOf(
                net.sourceforge.kolmafia.shop.ShopRow(
                    rowId = 1500,
                    item = net.sourceforge.kolmafia.shop.ItemStack(itemId = 99301, count = 1),
                    costs = listOf(net.sourceforge.kolmafia.shop.ItemStack(itemId = 99302, count = 75)),
                ),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            99302 to InventoryItem(99302, "FDKOL commendation", 100, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(99301, true));""").trim())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(99303, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun corpus_concShopVisitLearn_sessionLog() {
        registerCorpusItem(99801, "bottle of gin")
        registerCorpusItem(99802, "bottle of vodka")
        net.sourceforge.kolmafia.shop.ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "still\tNash Crosby's Still\tCONC\tSTILL\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = net.sourceforge.kolmafia.session.SessionLogger(prefs, GameEventBus())
        net.sourceforge.kolmafia.shop.ShopInventorySync.parseAndLearn(
            html = """
                <tr rel="99801">
                <a onClick='javascript:descitem(99801)'><b>bottle of gin</b></a>
                <span title="bottle of vodka"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=still&whichrow=700">
                </tr>
            """.trimIndent(),
            url = "shop.php?whichshop=still",
            sessionLogger = sessionLogger,
        )
        val log = prefs.getString(net.sourceforge.kolmafia.session.SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("bottle of gin\tSTILL, ROW700\tbottle of vodka"))
        net.sourceforge.kolmafia.shop.ShopRowDatabase.resetForTest()
    }

    @Test
    fun corpus_mysticVisitOverlayValidate_live() {
        registerCorpusItem(99901, "listed item A")
        registerCorpusItem(99902, "listed item B")
        registerCorpusItem(461, "red pixel")
        registerCorpusItem(459, "white pixel")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "mystic\tThe Crackpot Mystic's Shed\tCOIN\n",
            coinText = """
                The Crackpot Mystic's Shed\tROW100\tlisted item A\tred pixel (10)
                The Crackpot Mystic's Shed\tROW101\tlisted item B\twhite pixel (10)
            """.trimIndent(),
        )
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.registerVisitBuyRows(
            "mystic",
            listOf(
                net.sourceforge.kolmafia.shop.ShopRow(
                    rowId = 100,
                    item = net.sourceforge.kolmafia.shop.ItemStack(itemId = 99901, count = 1),
                    costs = listOf(net.sourceforge.kolmafia.shop.ItemStack(itemId = 461, count = 10)),
                ),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(level = "6"))
            },
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            461 to InventoryItem(461, "red pixel", 100, ItemType.OTHER),
                            459 to InventoryItem(459, "white pixel", 100, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(99901, true));""").trim())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(99902, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun corpus_flowertradeinVisitOverlayValidate_live() {
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "flowertradein\tThe Central Loathing Floral Mercantile Exchange\n",
            coinText = """
                The Central Loathing Floral Mercantile Exchange	buy	1	Chroner	ROW759
                The Central Loathing Floral Mercantile Exchange	buy	16	Chroner	ROW760
            """.trimIndent(),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            net.sourceforge.kolmafia.shop.FlowerTradeinAccessibility.ROSE to InventoryItem(
                                net.sourceforge.kolmafia.shop.FlowerTradeinAccessibility.ROSE,
                                "rose",
                                2,
                                ItemType.OTHER,
                            ),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(7567, true));""").trim())
        lib.processVisitResponseHooks(
            """
                <tr rel="7567">
                <a onClick='javascript:descitem(7567)'><b>Chroner</b></a>
                <span title="rose"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
                </tr>
            """.trimIndent(),
            "shop.php?whichshop=flowertradein",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(7567, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.replaceBuyRows(
            net.sourceforge.kolmafia.shop.FlowerTradeinSync.SHOP_ID,
            emptyList(),
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(7567, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun corpus_disabledCoinmasterVisitOverlay_live() {
        registerCorpusItem(99401, "disabled widget")
        registerCorpusItem(99402, "disabled token")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "disabledshop\tDisabled Test Shop\tCOIN\n",
            coinText = """
                Disabled Test Shop	buy	75	disabled widget	ROW1500
            """.trimIndent(),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.findByShopId("disabledshop")?.setDisabledForTest(true)
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            99402 to InventoryItem(99402, "disabled token", 100, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(99401, true));""").trim())
        lib.processVisitResponseHooks(
            """
                <tr rel="99401">
                <a onClick='javascript:descitem(99401)'><b>disabled widget</b></a>
                <span title="disabled token"><b>75</b></span>
                <form action="shop.php?action=buy&whichshop=disabledshop&whichrow=1500">
                </tr>
            """.trimIndent(),
            "shop.php?whichshop=disabledshop",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(99401, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun corpus_skillShopVisitOverlay_live() {
        net.sourceforge.kolmafia.data.SkillDefinitionDatabase.registerForTest(
            net.sourceforge.kolmafia.data.SkillDefinition(
                id = 6027,
                name = "Corpus Skill",
                image = "skillbook",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 99301,
                name = "shop token",
                descId = "99301",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "skillshop\tCorpus Skill Shop\tCOIN\n",
            coinText = """
                Corpus Skill Shop	buy	1	Other Skill	ROW2099
            """.trimIndent(),
        )
        net.sourceforge.kolmafia.shop.ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "skillshop\tCorpus Skill Shop\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            99301 to InventoryItem(99301, "shop token", 10, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        val charState = net.sourceforge.kolmafia.character.CharacterState(meat = 100_000)
        assertEquals("false", outputLib(lib, """print(is_coinmaster_skill(6027, true));""").trim())
        assertFalse(
            net.sourceforge.kolmafia.shop.CoinmasterDatabase.containsBuySkill(
                skillId = 6027,
                validate = true,
                state = charState,
                prefs = prefs,
                hasSkill = { false },
                accessibleCount = { if (it == 99301) 10 else 0 },
            ),
        )
        lib.processVisitResponseHooks(
            """
                <tr rel="99999">
                <td></td>
                <td><img src="itemimages/skillbook.gif" onclick="javascript:poop('desc_skill.php?whichskill=6027&amp;self=true','skill',350,300)"></td>
                <td><b>Corpus Skill</b></td>
                <td><img src="itemimages/token.gif" onclick="javascript:descitem(99301)"></td>
                <td><b>5</b></td>
                <td><a href="shop.php?action=buyitem&whichshop=skillshop&whichrow=2100">Buy</a></td>
                </tr>
            """.trimIndent(),
            "shop.php?whichshop=skillshop",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_skill(6027, true));""").trim())
        assertTrue(
            net.sourceforge.kolmafia.shop.CoinmasterDatabase.containsBuySkill(
                skillId = 6027,
                validate = true,
                state = charState,
                prefs = prefs,
                hasSkill = { false },
                accessibleCount = { if (it == 99301) 10 else 0 },
            ),
        )
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.replaceBuyRows("skillshop", emptyList())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_skill(6027, true));""").trim())
        assertFalse(
            net.sourceforge.kolmafia.shop.CoinmasterDatabase.containsBuySkill(
                skillId = 6027,
                validate = true,
                state = charState,
                prefs = prefs,
                hasSkill = { false },
                accessibleCount = { if (it == 99301) 10 else 0 },
            ),
        )
        net.sourceforge.kolmafia.data.SkillDefinitionDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.ShopRowDatabase.resetForTest()
    }

    @Test
    fun corpus_dynamicSkillShopVisit_live() {
        net.sourceforge.kolmafia.data.SkillDefinitionDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.ShopRowDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ItemDatabase.registerForTest(
            ItemData(
                id = 99302,
                name = "shop token",
                descId = "99302",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "skillshop\tCorpus Skill Shop\tCOIN\n",
            coinText = """
                Corpus Skill Shop	buy	1	Other Skill	ROW2099
            """.trimIndent(),
        )
        net.sourceforge.kolmafia.shop.ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "skillshop\tCorpus Skill Shop\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            99302 to InventoryItem(99302, "shop token", 10, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        val charState = net.sourceforge.kolmafia.character.CharacterState(meat = 100_000)
        assertFalse(
            net.sourceforge.kolmafia.shop.CoinmasterDatabase.containsBuySkill(
                skillId = 6027,
                validate = true,
                state = charState,
                prefs = prefs,
                hasSkill = { false },
                accessibleCount = { if (it == 99302) 10 else 0 },
            ),
        )
        lib.processVisitResponseHooks(
            """
                <tr rel="99999">
                <td></td>
                <td><img src="itemimages/skillbook.gif" onclick="javascript:poop('desc_skill.php?whichskill=6027&amp;self=true','skill',350,300)"></td>
                <td><b>Visit Learned Skill</b></td>
                <td><img src="itemimages/token.gif" onclick="javascript:descitem(99302)"></td>
                <td><b>5</b></td>
                <td><a href="shop.php?action=buyitem&whichshop=skillshop&whichrow=2100">Buy</a></td>
                </tr>
            """.trimIndent(),
            "shop.php?whichshop=skillshop",
        )
        assertEquals(
            "Visit Learned Skill",
            net.sourceforge.kolmafia.data.SkillDefinitionDatabase.getById(6027)?.name,
        )
        assertTrue(
            net.sourceforge.kolmafia.shop.CoinmasterDatabase.containsBuySkill(
                skillId = 6027,
                validate = true,
                state = charState,
                prefs = prefs,
                hasSkill = { false },
                accessibleCount = { if (it == 99302) 10 else 0 },
            ),
        )
        net.sourceforge.kolmafia.data.SkillDefinitionDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.ShopRowDatabase.resetForTest()
    }

    @Test
    fun corpus_legacySellShopVisitOverlay_live() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 99320,
                name = "legacy dime",
                descId = "99320",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 99321,
                name = "legacy junk",
                descId = "99321",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.registerForTest(
            net.sourceforge.kolmafia.shop.CoinmasterData(
                masterName = "Legacy Sell Shop",
                nickname = "legacysell",
                shopId = "legacysell",
                token = "legacy dime",
                buyItems = emptyList(),
                sellItems = listOf(
                    net.sourceforge.kolmafia.shop.ShopRow(
                        rowId = 99,
                        item = net.sourceforge.kolmafia.shop.ItemStack(itemId = 99321, count = 1),
                        price = 3,
                    ),
                ),
            ),
        )
        net.sourceforge.kolmafia.shop.ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "legacysell\tLegacy Sell Shop\tCOIN\n",
        )
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()),
            character = KoLCharacter(),
        )
        lib.processVisitResponseHooks(
            """
                <tr rel="99320">
                <a onClick='javascript:descitem(99320)'><b>legacy dime</b></a>
                <span title="legacy junk"><b>3</b></span>
                <form action="shop.php?action=sell&whichshop=legacysell&whichrow=2400">
                </tr>
            """.trimIndent(),
            "shop.php?whichshop=legacysell",
        )
        assertTrue(
            net.sourceforge.kolmafia.shop.CoinmasterDatabase.containsSellItem(
                itemId = 99321,
                validate = true,
                accessibleCount = { if (it == 99321) 1 else 0 },
            ),
        )
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.replaceSellRows("legacysell", emptyList())
        assertFalse(
            net.sourceforge.kolmafia.shop.CoinmasterDatabase.containsSellItem(
                itemId = 99321,
                validate = true,
                accessibleCount = { if (it == 99321) 1 else 0 },
            ),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        net.sourceforge.kolmafia.shop.ShopRowDatabase.resetForTest()
    }

    @Test
    fun corpus_arcadeLockedItemValidate_live() {
        registerCorpusItem(4637, "sinister demon mask")
        registerCorpusItem(4622, "Game Grid ticket")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "arcade\tArcade Ticket Counter\n",
            coinText = "Arcade Ticket Counter\tROW365\tsinister demon mask\tGame Grid ticket (1000)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setBoolean("lockedItem4637", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            4622 to InventoryItem(4622, "Game Grid ticket", 2000, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(4637, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<tr rel="4637"><td>sinister demon mask</td></tr>""",
            url = "shop.php?whichshop=arcade",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(4637, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_kiwiSpiritsValidate_live() {
        registerCorpusItem(11602, "mini kiwi intoxicating spirits")
        registerCorpusItem(11594, "mini kiwi")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "kiwi\tKiwi Kwiki Mart\n",
            coinText = "Kiwi Kwiki Mart\tROW1484\tmini kiwi intoxicating spirits\tmini kiwi (3)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            11594 to InventoryItem(11594, "mini kiwi", 10, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(11602, true));""").trim())
        lib.processVisitResponseHooks(
            html = "<html></html>",
            url = "shop.php?whichshop=kiwi",
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(11602, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_fiveDPrinterUnknownRecipeValidate_live() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 7750,
                name = "Xiblaxian 5D printer",
                descId = "7750",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 7733,
                name = "Xiblaxian circuitry",
                descId = "7733",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 7752,
                name = "Xiblaxian xenogoggles",
                descId = "147449485",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "5dprinter\tXiblaxian 5D printer\n",
            coinText = "Xiblaxian 5D printer\tROW337\tXiblaxian xenogoggles\tXiblaxian circuitry (4)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setBoolean("unknownRecipe7752", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            7750 to InventoryItem(7750, "Xiblaxian 5D printer", 1, ItemType.OTHER),
                            7733 to InventoryItem(7733, "Xiblaxian circuitry", 10, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_item(7752, true));""").trim())
        lib.processVisitResponseHooks(
            html = """<a onclick="descitem(147449485);">goggles</a>""",
            url = "shop.php?whichshop=5dprinter",
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(7752, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun corpus_learnedShopRowsRestoreValidate_live() {
        registerCorpusItem(99001, "visit-learned item")
        registerCorpusItem(99002, "FDKOL commendation")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "fdkol\tFDKOL Requisitions Tent\tNPCCOIN\n",
            coinText = "FDKOL Requisitions Tent\tROW1500\tvisit-learned item\tFDKOL commendation (75)\n",
        )
        net.sourceforge.kolmafia.shop.ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val visitRow = net.sourceforge.kolmafia.shop.ShopRow(
            rowId = 1500,
            item = net.sourceforge.kolmafia.shop.ItemStack(itemId = 99001, count = 1),
            costs = listOf(net.sourceforge.kolmafia.shop.ItemStack(itemId = 99002, count = 75)),
        )
        net.sourceforge.kolmafia.shop.ShopRowDatabase.registerVisitRow(1500, "fdkol", visitRow)
        net.sourceforge.kolmafia.shop.ShopRowDatabase.persistLearnedRow(prefs, 1500, "fdkol", visitRow)

        net.sourceforge.kolmafia.shop.ShopRowDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterVisitInventory.resetForTest()
        ItemDatabase.resetForTest()
        registerCorpusItem(99001, "visit-learned item")
        registerCorpusItem(99002, "FDKOL commendation")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "fdkol\tFDKOL Requisitions Tent\tNPCCOIN\n",
            coinText = "FDKOL Requisitions Tent\tROW1500\tvisit-learned item\tFDKOL commendation (75)\n",
        )
        net.sourceforge.kolmafia.shop.ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        net.sourceforge.kolmafia.shop.ShopRowDatabase.restoreLearnedRows(prefs)

        val lib = GameRuntimeLibrary(
            preferences = prefs,
            inventoryManager = object : InventoryManager(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
                GameEventBus(),
            ) {
                private val flow = MutableStateFlow(
                    InventoryState(
                        items = mapOf(
                            99002 to InventoryItem(99002, "FDKOL commendation", 100, ItemType.OTHER),
                        ),
                    ),
                )
                override val state = flow.asStateFlow()
            },
        )
        assertEquals("true", outputLib(lib, """print(is_coinmaster_item(99001, true));""").trim())
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.ShopRowDatabase.resetForTest()
    }

    @Test
    fun corpus_septemberEmberVisitSync_live() {
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "september\tSept-Ember Censer\n",
            coinText = "Sept-Ember Censer\tbuy\t1\tember item\tROW1510\n",
        )
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks(
            html = """<b>You have 1,234 Embers.</b>""",
            url = "shop.php?whichshop=september",
        )
        assertTrue(prefs.getBoolean(net.sourceforge.kolmafia.shop.SeptEmberSync.BALANCE_CHECKED_PREF, false))
        assertEquals(1234, prefs.getInt(net.sourceforge.kolmafia.shop.SeptEmberSync.AVAILABLE_EMBERS_PREF, 0))
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_chronerTowerVisitHook_live() {
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "shakeshop\tShake Shop\n",
            coinText = "Shake Shop\tbuy\t1\tshake\tROW1\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(net.sourceforge.kolmafia.shop.TimeTowerSync.PREF, false)
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks(
            html = """<b>Shake Shop</b> open for business""",
            url = "shop.php?whichshop=shakeshop",
        )
        assertTrue(prefs.getBoolean(net.sourceforge.kolmafia.shop.TimeTowerSync.PREF, false))
        lib.processVisitResponseHooks(
            html = """That store isn't there anymore.""",
            url = "shop.php?whichshop=shakeshop",
        )
        assertFalse(prefs.getBoolean(net.sourceforge.kolmafia.shop.TimeTowerSync.PREF, true))
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_trapperYakSkinValidate_live() {
        registerCorpusItem(394, "yak skin")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "trapper\tThe Trapper\n",
            coinText = "The Trapper\tbuy\t1\tyak skin\tROW14\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(
                    CharacterApiResponse(
                        meat = "100000",
                        level = "10",
                        ascensions = "4",
                    ),
                )
            },
        )
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item(394, true));""").trim(),
        )
        lib.processVisitResponseHooks(
            html = """trade your yeti furs for goods""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=trapper",
        )
        assertEquals(4, prefs.getInt("lastTr4pz0rQuest", -1))
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item(394, true));""").trim(),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_latheVisitPref_live() {
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "lathe\tYour SpinMaster lathe\n",
            coinText = "Your SpinMaster lathe\tbuy\t1\tlathe item\tROW1\n",
        )
        val prefs = Preferences(MapSettings())
        assertFalse(prefs.getBoolean(net.sourceforge.kolmafia.shop.SpinMasterLatheSync.VISITED_PREF, false))
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks(
            html = """<b>SpinMaster lathe</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=lathe",
        )
        assertTrue(prefs.getBoolean(net.sourceforge.kolmafia.shop.SpinMasterLatheSync.VISITED_PREF, false))
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_junkmagazineHippyQuest_live() {
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "junkmagazine\tWorse Homes and Gardens\n",
            coinText = "Worse Homes and Gardens\tbuy\t1\tjunk item\tROW1\n",
        )
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks(
            html = """<b>Worse Homes and Gardens</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=junkmagazine",
        )
        assertEquals(
            "step2",
            prefs.getString(net.sourceforge.kolmafia.quest.Quest.HIPPY.prefKey, "unstarted"),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
    }

    @Test
    fun corpus_getRelatedPulverize_live() {
        registerCorpusWeapon(BEJEWELED_CUFFLINKS, "bejeweled cufflinks")
        registerCorpusItem(HOT_NUGGETS, "hot nuggets")
        EquipmentDatabase.loadPulverizeFromText(
            """
                bejeweled cufflinks	258112
            """.trimIndent(),
        )
        val lib = GameRuntimeLibrary()
        val out = outputLib(
            lib,
            """
                item i = to_item("bejeweled cufflinks");
                print(contains_key(get_related(i, "pulverize"), to_item("hot nuggets")));
            """.trimIndent(),
        )
        assertEquals("true", out.trim())
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
    }

    @Test
    fun corpus_pulverizeCli_live() = runBlocking {
        val calls = mutableListOf<Pair<Int, Int>>()
        val pulverize = object : net.sourceforge.kolmafia.request.PulverizeRequest(
            HttpClient(MockEngine { respond("Smash!", HttpStatusCode.OK) }),
        ) {
            override suspend fun pulverize(itemId: Int, quantity: Int): Result<Int> {
                calls += itemId to quantity
                return Result.success(quantity)
            }
        }
        registerCorpusWeapon(PULVERIZE_ITEM, "corpus sword")
        val inventory = object : InventoryManager(
            HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(items = mapOf(PULVERIZE_ITEM to InventoryItem(PULVERIZE_ITEM, "corpus sword", 2, ItemType.WEAPON))),
            )
            override val state = flow.asStateFlow()
        }
        val db = object : GameDatabase() {
            override fun item(name: String) = ItemDatabase.getByName(name)
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            pulverizeRequest = pulverize,
            inventoryManager = inventory,
        )
        runLib(lib, """cli_execute("pulverize 1 corpus sword");""")
        assertEquals(listOf(PULVERIZE_ITEM to 1), calls)
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
    }

    @Test
    fun corpus_getRelatedFold_live() {
        registerCorpusItem(FOLD_A, "corpus fold a")
        registerCorpusItem(FOLD_B, "corpus fold b")
        net.sourceforge.kolmafia.data.FoldGroupDatabase.registerGroupForTest(
            net.sourceforge.kolmafia.data.FoldGroup(
                hpDamagePct = 0,
                items = listOf("corpus fold a", "corpus fold b"),
            ),
        )
        val lib = GameRuntimeLibrary()
        val out = outputLib(
            lib,
            """
                item i = to_item("corpus fold a");
                print(get_related(i, "fold")[to_item("corpus fold b")]);
            """.trimIndent(),
        )
        assertEquals("2", out.trim())
        ItemDatabase.resetForTest()
        net.sourceforge.kolmafia.data.FoldGroupDatabase.resetForTest()
    }

    @Test
    fun corpus_getRelatedZap_live() {
        registerCorpusItem(ZAP_A, "corpus zap a")
        registerCorpusItem(ZAP_B, "corpus zap b")
        net.sourceforge.kolmafia.data.ZapGroupDatabase.registerGroupForTest(
            listOf("corpus zap a", "corpus zap b"),
        )
        val lib = GameRuntimeLibrary()
        val out = outputLib(
            lib,
            """
                item i = to_item("corpus zap a");
                print(contains_key(get_related(i, "zap"), to_item("corpus zap b")));
                print(get_related(i, "zap")[to_item("corpus zap b")]);
            """.trimIndent(),
        )
        assertEquals(
            """
                true
                0
            """.trimIndent(),
            out.trim(),
        )
        ItemDatabase.resetForTest()
        net.sourceforge.kolmafia.data.ZapGroupDatabase.resetForTest()
    }

    @Test
    fun corpus_getZapWand_live() {
        registerCorpusItem(WandDiscovery.PINE_WAND, "corpus pine wand")
        val inventory = object : InventoryManager(
            io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                respond("ok", io.ktor.http.HttpStatusCode.OK)
            }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        WandDiscovery.PINE_WAND to InventoryItem(
                            WandDiscovery.PINE_WAND,
                            "corpus pine wand",
                            1,
                            ItemType.OTHER,
                        ),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }
        val lib = GameRuntimeLibrary(
            inventoryManager = inventory,
            preferences = prefs(),
        )
        val out = outputLib(lib, "print(to_string(get_zap_wand()));")
        assertEquals("corpus pine wand", out.trim())
        ItemDatabase.resetForTest()
    }

    @Test
    fun corpus_zap_live() {
        registerCorpusItem(WandDiscovery.PINE_WAND, "corpus pine wand")
        registerCorpusItem(CORPUS_ZAP_SOURCE, "corpus zap source")
        registerCorpusItem(CORPUS_ZAP_TARGET, "corpus zap target")
        val inventory = object : InventoryManager(
            io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                respond("ok", io.ktor.http.HttpStatusCode.OK)
            }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            private val flow = kotlinx.coroutines.flow.MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        WandDiscovery.PINE_WAND to InventoryItem(
                            WandDiscovery.PINE_WAND,
                            "corpus pine wand",
                            1,
                            ItemType.OTHER,
                        ),
                        CORPUS_ZAP_SOURCE to InventoryItem(
                            CORPUS_ZAP_SOURCE,
                            "corpus zap source",
                            1,
                            ItemType.OTHER,
                        ),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
            override fun consumeItemLocally(itemId: Int, quantity: Int) {
                if (quantity <= 0) return
                val current = flow.value
                val item = current.items[itemId] ?: return
                val remaining = item.quantity - quantity
                val updated = current.items.toMutableMap()
                if (remaining <= 0) {
                    updated.remove(itemId)
                } else {
                    updated[itemId] = item.copy(quantity = remaining)
                }
                flow.value = current.copy(items = updated)
            }
        }
        val engine = MockEngine {
            respond(
                "You acquire an item: <b>corpus zap target</b>",
                HttpStatusCode.OK,
            )
        }
        val zapRequest = ZapRequest(
            client = HttpClient(engine),
            inventoryManager = inventory,
            preferences = prefs(),
        )
        val lib = GameRuntimeLibrary(
            inventoryManager = inventory,
            zapRequest = zapRequest,
            preferences = prefs(),
        )
        val out = outputLib(
            lib,
            """
                item result = zap(to_item("corpus zap source"));
                print(to_string(result));
            """.trimIndent(),
        )
        assertEquals("corpus zap target", out.trim())
        ItemDatabase.resetForTest()
    }

    private fun registerCorpusWeapon(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        EquipmentDatabase.registerForTest(
            id,
            net.sourceforge.kolmafia.data.EquipmentData(name, 120, null, 1, "sword"),
        )
    }

    private fun emptyInventoryManager(): InventoryManager =
        object : InventoryManager(
            io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
                respond("ok", io.ktor.http.HttpStatusCode.OK)
            }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            private val flow = kotlinx.coroutines.flow.MutableStateFlow(InventoryState())
            override val state = flow.asStateFlow()
        }

    private fun registerCorpusItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun corpusSkillManager(vararg skillIds: Int): SkillManager {
        val skills = skillIds.map { id ->
            SkillData(id, "Skill $id", SkillType.PASSIVE, mpCost = 0, dailyLimit = 0, timesCast = 0)
        }
        val json = "{" + skills.joinToString(",") { s ->
            """"${s.id}":{"name":"${s.name}","type":5,"dailylimit":0,"timescast":0,"mpcost":0}"""
        } + "}"
        val engine = MockEngine {
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return SkillManager(client, SkillCastRequest(client), GameEventBus()).also { mgr ->
            runBlocking { mgr.fetchSkills() }
        }
    }

    companion object {
        private const val BEJEWELED_CUFFLINKS = 3958
        private const val HOT_NUGGETS = 1445
        private const val PULVERIZE_ITEM = 9101
        private const val FOLD_A = 9102
        private const val FOLD_B = 9103
        private const val ZAP_A = 9104
        private const val ZAP_B = 9105
        private const val CORPUS_ZAP_SOURCE = 9106
        private const val CORPUS_ZAP_TARGET = 9107
    }
}
