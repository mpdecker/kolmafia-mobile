package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.MonsterManuelManager
import net.sourceforge.kolmafia.session.TurnCounter

/**
 * Focused XLV Track C coverage (phases 6591–6610).
 * REVISION not bumped (stays phase6850); get_revision ASH is INT.
 */
class GameRuntimeLibraryPhase6591Test {

    @BeforeTest
    fun setUp() {
        ChoiceCombatAshState.reset()
    }

    @AfterTest
    fun tearDown() {
        ChoiceCombatAshState.reset()
    }

    @Test
    fun get_revision_returnsIntPhaseDigits() {
        assertEquals("phase6910", GameRuntimeLibrary.REVISION)
        assertEquals(6730, GameRuntimeLibrary.revisionNumber())
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
        assertEquals(
            "6850",
            outputLib(GameRuntimeLibrary(), "int r = get_revision(); print(r);"),
        )
    }

    @Test
    fun get_counter_skipsExemptWhenLabelEmpty() {
        val p = Preferences(MapSettings())
        TurnCounter.startCounting(p, currentRun = 10, turns = 5, "Hidden loc=*", "x.gif")
        TurnCounter.startCounting(p, currentRun = 10, turns = 7, "Visible", "y.gif")
        val char = KoLCharacter().also { it.setCurrentRun(10) }
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        // Empty label: first non-exempt (Visible), not Hidden loc=*
        assertEquals("7", outputLib(lib, """print(get_counter(""));""").trim())
        assertEquals("7", outputLib(lib, """print(get_counter("Visible"));""").trim())
        // Named lookup still finds exempt counters
        assertEquals("5", outputLib(lib, """print(get_counter("Hidden"));""").trim())
        val labels = outputLib(lib, """print(get_counters("", 0, 20));""")
        assertTrue("Visible" in labels)
        assertFalse("Hidden" in labels)

        // Only exempt → -1
        val p2 = Preferences(MapSettings())
        TurnCounter.startCounting(p2, currentRun = 0, turns = 3, "Solo loc=*", "z.gif")
        val lib2 = GameRuntimeLibrary(preferences = p2, character = KoLCharacter())
        assertEquals("-1", outputLib(lib2, """print(get_counter(""));""").trim())
    }

    @Test
    fun session_logs_negativeThrows_andZeroEmpty() {
        assertFailsWith<ScriptException> {
            outputLib(GameRuntimeLibrary(), "session_logs(-1);")
        }
        assertFailsWith<ScriptException> {
            outputLib(GameRuntimeLibrary(), """session_logs("TestPlayer", -2);""")
        }
        assertEquals("0", outputLib(GameRuntimeLibrary(), "print(count(session_logs(0)));"))
    }

    @Test
    fun get_property_blocksNonEditable() {
        val p = Preferences(MapSettings()).also {
            it.setString("externalEditor", "vim")
            it.setString("okKey", "yes")
        }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("", outputLib(lib, """print(get_property("externalEditor"));"""))
        assertEquals("yes", outputLib(lib, """print(get_property("okKey"));"""))
        assertEquals("", outputLib(lib, """print(remove_property("saveState.foo"));"""))
    }

    @Test
    fun form_fields_decodesLastVisitAndPost() {
        ChoiceCombatAshState.setFormFieldsFromPostData("word=hello%20world&flag=a%2Bb")
        val lib = GameRuntimeLibrary()
        assertEquals("hello world", outputLib(lib, """print(form_fields()["word"]);"""))
        assertEquals("a+b", outputLib(lib, """print(form_fields()["flag"]);"""))

        ChoiceCombatAshState.reset()
        lib.lastVisitPath = "choice.php?whichchoice=1&msg=hi%20there"
        assertEquals("hi there", outputLib(lib, """print(form_fields()["msg"]);"""))
    }

    @Test
    fun is_banished_monsterAndPhylumLive() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals(
            "false",
            outputLib(lib, """print(is_banished(to_monster("none")));""").trim().lowercase(),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(is_banished(to_phylum("beast"))));""").trim().lowercase(),
        )
    }

    @Test
    fun monster_factoids_cachedOnly() {
        val monster = MonsterDatabase.getById(1) ?: return
        MonsterManuelManager.registerMonster(
            monster.id,
            """<ul><li>fact one<li>fact two</ul>""",
        )
        val lib = GameRuntimeLibrary()
        assertEquals(
            "2",
            outputLib(
                lib,
                """print(monster_factoids_available(to_monster("${monster.name}"), true));""",
            ),
        )
    }

    @Test
    fun sweet_synthesis_nonCandyReturnsFalse() {
        assertEquals(
            "false",
            outputLib(
                GameRuntimeLibrary(),
                """print(sweet_synthesis(to_item("none"), to_item("none")));""",
            ).trim().lowercase(),
        )
    }
}
