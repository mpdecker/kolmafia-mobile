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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.combat.EncounterModifierPipeline
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.concoction.StillsAvailability
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.DescriptionCache
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.request.CharacterRequest
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
        registerCorpusItem(9901, "floundry fish")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "floundry fish",
                resultQuantity = 1,
                methods = setOf("FLOUNDRY"),
                ingredients = emptyList(),
            ),
        )
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("false", outputLib(lib, """print(is_craft_permitted(9901));""").trim())
        prefs.setBoolean(net.sourceforge.kolmafia.clan.ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true)
        assertEquals("true", outputLib(lib, """print(is_craft_permitted(9901));""").trim())
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
}
