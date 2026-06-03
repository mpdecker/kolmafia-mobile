package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryTest {

    // Uses a minimal stub that provides no game managers — only pure utility functions are tested here.
    private fun run(src: String): AshRuntime {
        val runtime = AshRuntime(GameRuntimeLibrary.forTesting())
        runtime.execute(AshParser().parse(src))
        return runtime
    }

    private fun output(src: String) = run(src).output.toString().trim()

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
}
