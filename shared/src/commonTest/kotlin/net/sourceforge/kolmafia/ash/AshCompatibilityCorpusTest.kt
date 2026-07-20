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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.maximizer.MaximizerManager
import net.sourceforge.kolmafia.preferences.Preferences
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
        assertTrue(outputLib(lib, """print(to_string(my_path_id()));""").trim().toLongOrNull() != null)
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
            "cold",
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
}
