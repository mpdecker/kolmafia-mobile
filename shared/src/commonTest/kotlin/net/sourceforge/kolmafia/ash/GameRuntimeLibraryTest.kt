package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.FamiliarDefinition
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryTest {

    // Runs ASH source with a given KoLCharacter injected into the library.
    private fun runWithCharacter(character: KoLCharacter, src: String): String {
        val runtime = AshRuntime(GameRuntimeLibrary(character = character))
        runtime.execute(AshParser().parse(src))
        return runtime.output.toString().trim()
    }

    // Uses a minimal stub that provides no game managers — only pure utility functions are tested here.
    private fun run(src: String): AshRuntime = runLib(GameRuntimeLibrary.forTesting(), src)

    private fun output(src: String): String = outputLib(GameRuntimeLibrary.forTesting(), src)

    private fun prefs(): Preferences = Preferences(MapSettings())

    // --- Type conversion ---

    @Test
    fun to_string_int() = assertEquals("42", output("print(to_string(42));"))

    @Test
    fun to_string_float() = assertEquals("3.14", output("print(to_string(3.14));"))

    @Test
    fun to_string_boolean() = assertEquals("true", output("print(to_string(true));"))

    @Test
    fun to_int_fromString() = assertEquals("7", output("""print(to_string(to_int("7")));"""))

    @Test
    fun to_int_fromFloat() = assertEquals("3", output("print(to_string(to_int(3.9)));"))

    @Test
    fun to_float_fromInt() = assertEquals("5.0", output("print(to_string(to_float(5)));"))

    @Test
    fun to_boolean_fromInt_zero() = assertEquals("false", output("print(to_string(to_boolean(0)));"))

    @Test
    fun to_boolean_fromInt_nonzero() = assertEquals("true", output("print(to_string(to_boolean(1)));"))

    // --- String utilities ---

    @Test
    fun length_string() = assertEquals("5", output("""print(to_string(length("hello")));"""))

    @Test
    fun length_emptyString() = assertEquals("0", output("""print(to_string(length("")));"""))

    @Test
    fun substring_basic() = assertEquals("ell", output("""print(substring("hello", 1, 3));"""))

    @Test
    fun index_of_found() = assertEquals("2", output("""print(to_string(index_of("hello", "ll")));"""))

    @Test
    fun index_of_notFound() = assertEquals("-1", output("""print(to_string(index_of("hello", "xyz")));"""))

    @Test
    fun to_upper_case() = assertEquals("HELLO", output("""print(to_upper_case("hello"));"""))

    @Test
    fun to_lower_case() = assertEquals("hello", output("""print(to_lower_case("HELLO"));"""))

    @Test
    fun starts_with_true() = assertEquals("true", output("""print(to_string(starts_with("hello", "he")));"""))

    @Test
    fun starts_with_false() = assertEquals("false", output("""print(to_string(starts_with("hello", "lo")));"""))

    @Test
    fun replace_string() = assertEquals("heXXo", output("""print(replace_string("hello", "l", "X"));"""))

    @Test
    fun split_string() {
        val o = output("""
            string[int] parts = split_string("a,b,c", ",");
            print(parts[0]);
            print(parts[1]);
            print(parts[2]);
        """.trimIndent())
        assertEquals("a\nb\nc", o)
    }

    // --- Math ---

    @Test
    fun math_floor() = assertEquals("3", output("print(to_string(floor(3.9)));"))

    @Test
    fun math_ceil() = assertEquals("4", output("print(to_string(ceil(3.1)));"))

    @Test
    fun math_round() = assertEquals("4", output("print(to_string(round(3.6)));"))

    @Test
    fun math_abs_negative() = assertEquals("5", output("print(to_string(abs(-5)));"))

    @Test
    fun math_abs_float() = assertEquals("2.5", output("print(to_string(abs(-2.5)));"))

    @Test
    fun math_sqrt() {
        run("float r = sqrt(9.0);") // just verify no throw
        assertTrue(true)
    }

    @Test
    fun math_max_int() = assertEquals("7", output("print(to_string(max(3, 7)));"))

    @Test
    fun math_min_int() = assertEquals("3", output("print(to_string(min(3, 7)));"))

    @Test
    fun math_random() {
        val runtime = AshRuntime(GameRuntimeLibrary.forTesting())
        val nodes = AshParser().parse("float r = random(1.0);")
        runtime.execute(nodes)
        assertTrue(true)
    }

    // --- Aggregate utilities ---

    @Test
    fun count_aggregate() = assertEquals(
        "3",
        output("""
            string[int] m; m[0]="a"; m[1]="b"; m[2]="c";
            print(to_string(count(m)));
        """.trimIndent())
    )

    @Test
    fun clear_aggregate() = assertEquals(
        "0",
        output("""
            string[int] m; m[0]="a"; m[1]="b";
            clear(m);
            print(to_string(count(m)));
        """.trimIndent())
    )

    @Test
    fun abort_throwsScriptExceptionWithMessage() {
        val ex = kotlin.runCatching { run("""abort("bad item");""") }.exceptionOrNull()
        assertTrue(ex is ScriptException)
        assertTrue(ex!!.message!!.contains("bad item"))
    }

    // --- Familiar ---

    @Test
    fun myFamiliar_noFamiliar_zeroId_returnsNone() {
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(name = "PlayerName", familiar = "0", familiarname = "Ghostly Apparition")
        )
        assertEquals("none", runWithCharacter(character, "print(to_string(my_familiar()));"))
    }

    @Test
    fun myFamiliar_withActiveFamiliar_returnsRace() {
        val parrot = FamiliarData(
            id = 7, name = "Polly", race = "Exotic Parrot",
            weight = 5, experience = 0, kills = 0
        )
        val fm = FamiliarManager(HttpClient(MockEngine { respond("") }), GameEventBus())
        fm.testSetState(FamiliarState(activeFamiliar = parrot))
        val runtime = AshRuntime(GameRuntimeLibrary(familiarManager = fm))
        runtime.execute(AshParser().parse("print(to_string(my_familiar()));"))
        assertEquals("Exotic Parrot", runtime.output.toString().trim())
    }

    // --- Banish queries ---

    private fun runWithBanishManager(banishManager: BanishManager?, src: String): AshRuntime {
        val lib = GameRuntimeLibrary(banishManager = banishManager)
        val runtime = AshRuntime(lib)
        runtime.execute(AshParser().parse(src))
        return runtime
    }

    private fun outputWithBanishManager(banishManager: BanishManager?, src: String) =
        runWithBanishManager(banishManager, src).output.toString().trim()

    @Test
    fun isBanished_banishedMonster_returnsTrue() {
        val mgr = BanishManager(prefs())
        mgr.banishMonster("Goblin", Banisher.SNOKEBOMB, 0)
        val result = outputWithBanishManager(mgr, """print(to_string(is_banished("Goblin")));""")
        assertEquals("true", result)
    }

    @Test
    fun isBanished_unknownMonster_returnsFalse() {
        val mgr = BanishManager(prefs())
        val result = outputWithBanishManager(mgr, """print(to_string(is_banished("Goblin")));""")
        assertEquals("false", result)
    }

    @Test
    fun isBanished_noManager_returnsFalse() {
        val result = outputWithBanishManager(null, """print(to_string(is_banished("Goblin")));""")
        assertEquals("false", result)
    }

    @Test
    fun banishersUsed_returnsBanishedMonsters() {
        val mgr = BanishManager(prefs())
        mgr.banishMonster("goblin", Banisher.SNOKEBOMB, 0)
        val result = outputWithBanishManager(mgr, """
            string[monster] b = banishers_used();
            monster g = to_monster("goblin");
            print(b[g]);
        """.trimIndent())
        assertEquals(Banisher.SNOKEBOMB.canonicalName, result)
    }

    @Test
    fun banishersUsed_noManager_returnsEmpty() {
        val result = outputWithBanishManager(null, """
            string[monster] b = banishers_used();
            print(to_string(count(b)));
        """.trimIndent())
        assertEquals("0", result)
    }

    // ── to_int entity overloads ──────────────────────────────────────────────────
    // Note: the ASH interpreter in this codebase does not support $type[name] literal
    // syntax. Tests use to_item(string), to_effect(string), etc. to construct entity
    // values instead, which is the same code path as far as to_int() is concerned.

    private fun itemDb(itemId: Int): GameDatabase = object : GameDatabase() {
        override fun item(name: String) = ItemData(
            id = itemId, name = name, descId = "", image = "",
            primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
            access = emptySet(), autosellPrice = 0, plural = null
        )
    }

    private fun effectDb(effectId: Int): GameDatabase = object : GameDatabase() {
        override fun effect(name: String) = EffectData(
            id = effectId, name = name, image = "", descId = "",
            quality = EffectQuality.UNKNOWN, attributes = emptySet()
        )
    }

    private fun skillDb(skillId: Int): GameDatabase = object : GameDatabase() {
        override fun skill(name: String) = SkillDefinition(
            id = skillId, name = name, image = "", tags = emptySet(),
            mpCost = 0, duration = 0,
            isPassive = false, isCombat = false, isNonCombat = false, isSong = false
        )
    }

    private fun familiarDb(familiarId: Int): GameDatabase = object : GameDatabase() {
        override fun familiar(name: String) = FamiliarDefinition(
            id = familiarId, name = name, image = "", types = emptySet(),
            larvaItem = "", hatchlingItem = "",
            arenaCombatMoves = 0, arenaStrength = 0, arenaOc = 0, arenaHs = 0,
            attributes = emptySet()
        )
    }

    private fun monsterDb(monsterId: Int): GameDatabase = object : GameDatabase() {
        override fun monster(name: String) = MonsterDefinition(
            name = name, id = monsterId, image = "",
            attack = 0, defense = 0, hp = 0, initiative = 0, meatDrop = 0,
            phylum = "dude", isBoss = false, isGhost = false, isLucky = false,
            isScaling = false, scale = 0, cap = 0, floor = 0, drops = emptyList()
        )
    }

    private fun zoneDb(snarfblat: Int): GameDatabase = object : GameDatabase() {
        override fun zone(locationName: String) = AdventureZone(
            zoneName = locationName, urlParams = "adventure=$snarfblat",
            locationName = locationName, environment = "", diffLevel = "",
            statRequirement = 0, goals = emptyList(),
            isOverdrunk = false, noWander = false
        )
    }

    @Test
    fun to_int_fromItem_returnsItemId() {
        val lib = GameRuntimeLibrary(gameDatabase = itemDb(42))
        assertEquals("42", outputLib(lib, """print(to_string(to_int(to_item("Seal Tooth"))));"""))
    }

    @Test
    fun to_int_fromEffect_returnsEffectId() {
        val lib = GameRuntimeLibrary(gameDatabase = effectDb(55))
        assertEquals("55", outputLib(lib, """print(to_string(to_int(to_effect("Beaten Up"))));"""))
    }

    @Test
    fun to_int_fromSkill_returnsSkillId() {
        val lib = GameRuntimeLibrary(gameDatabase = skillDb(28))
        assertEquals("28", outputLib(lib, """print(to_string(to_int(to_skill("Empathy of the Newt"))));"""))
    }

    @Test
    fun to_int_fromFamiliar_returnsFamiliarId() {
        val lib = GameRuntimeLibrary(gameDatabase = familiarDb(7))
        assertEquals("7", outputLib(lib, """print(to_string(to_int(to_familiar("Mosquito"))));"""))
    }

    @Test
    fun to_int_fromMonster_returnsMonsterId() {
        val lib = GameRuntimeLibrary(gameDatabase = monsterDb(17))
        assertEquals("17", outputLib(lib, """print(to_string(to_int(to_monster("Knob Goblin"))));"""))
    }

    @Test
    fun to_int_fromLocation_returnsSnarfblat() {
        val lib = GameRuntimeLibrary(gameDatabase = zoneDb(88))
        assertEquals("88", outputLib(lib, """print(to_string(to_int(to_location("The Haunted Ballroom"))));"""))
    }

    @Test
    fun to_int_fromItem_returnsZeroWhenDbNull() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("0", outputLib(lib, """print(to_string(to_int(to_item("unknown item"))));"""))
    }

    @Test
    fun to_location_fromString_returnsLocation() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(
            "The Haunted Pantry",
            outputLib(lib, """print(to_string(to_location("The Haunted Pantry")));""")
        )
    }

    @Test
    fun war_progress_readsSyncedPreference() {
        val prefs = Preferences(MapSettings())
        prefs.setString("warProgress", "started")
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("started", outputLib(lib, """print(war_progress());"""))
    }

    @Test
    fun cliWar_echoesWarProgress() {
        val prefs = Preferences(MapSettings())
        prefs.setString("warProgress", "finished")
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("finished", outputLib(lib, """cli_execute("war");""").trim())
    }

    @Test
    fun ns_challenge_readsPreference() {
        val prefs = Preferences(MapSettings())
        prefs.setString("nsChallenge2", "cold")
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("cold", outputLib(lib, """print(ns_challenge(2));"""))
    }
}
