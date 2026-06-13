package net.sourceforge.kolmafia.ash

import kotlin.test.Test

/**
 * Minimal snippets from common automation patterns.
 * Each must parse and run without ScriptException when managers are stubbed.
 */
class AshCompatibilityCorpusTest {

    private val lib get() = GameRuntimeLibrary(preferences = prefs())

    private val snippets = listOf(
        """int x = 0; while (x < 3) { x++; } print(to_string(x));""",
        """int[int] m; foreach i in m { print(to_string(i)); }""",
        """try { int y = 1; } catch { print("caught"); }""",
        """print(to_string(to_int(to_item("seal tooth"))));""",
        """set_property("_test", "1"); print(get_property("_test"));""",
        """wait(0); waitq(0);""",
        """print(to_string(goal_exists("item")));""",
        """cli_execute("echo hello");""",
        """print(to_string(runscript("nonexistent")));""",
        """string[int] a; a[0]="x"; a[1]="y"; print(join_string(a, "|"));""",
        """int[item] m; m[to_item("seal tooth")] = 1; print(to_string(contains_key(m, to_item("seal tooth"))));""",
        """print(to_string(to_path("none")));""",
        """print(to_string(my_stat(to_stat("muscle"))));""",
        """print(to_string(numeric_modifier(to_skill("Abdominal Muscles"), "Muscle")));""",
        """print(form_field("<form><input name=\"x\" value=\"y\"></form>", "x"));""",
        """string[string] p; p["a"] = "1"; print(make_url("campground.php", p));""",
    )

    @Test
    fun corpus_allSnippetsRunWithoutScriptException() {
        snippets.forEach { src -> runLib(lib, src) }
    }
}
