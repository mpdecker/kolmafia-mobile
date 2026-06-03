package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class AshRuntimeTest {

    private fun run(src: String): AshRuntime {
        val runtime = AshRuntime(RuntimeLibrary())
        runtime.execute(AshParser().parse(src))
        return runtime
    }

    private fun output(src: String): String = run(src).output.toString().trim()

    // --- Arithmetic ---

    @Test fun eval_intAddition() = assertEquals("3", output("print(to_string(1 + 2));"))
    @Test fun eval_intMultiplication() = assertEquals("6", output("print(to_string(2 * 3));"))
    @Test fun eval_operatorPrecedence() = assertEquals("7", output("print(to_string(1 + 2 * 3));"))
    @Test fun eval_floatArithmetic() = assertEquals("2.5", output("print(to_string(5.0 / 2.0));"))
    @Test fun eval_stringConcatenation() = assertEquals("hello world", output("""print("hello" + " " + "world");"""))
    @Test fun eval_divisionByZero_throws() { assertFails { run("int x = 1 / 0;") } }

    // --- Variables ---

    @Test fun eval_varDecl_withInit() = assertEquals("42", output("int x = 42; print(to_string(x));"))
    @Test fun eval_varDecl_defaultValue() = assertEquals("0", output("int x; print(to_string(x));"))
    @Test fun eval_varAssignment() = assertEquals("10", output("int x = 5; x = x * 2; print(to_string(x));"))
    @Test fun eval_compoundAssignment_plusEq() = assertEquals("8", output("int x = 5; x += 3; print(to_string(x));"))
    @Test fun eval_preIncrement() = assertEquals("6", output("int x = 5; ++x; print(to_string(x));"))
    @Test fun eval_postIncrement_returnsOld() {
        assertEquals("5", output("int x = 5; int y = x++; print(to_string(y));"))
        assertEquals("6", output("int x = 5; x++; print(to_string(x));"))
    }

    // --- Control flow ---

    @Test fun eval_if_taken() = assertEquals("yes", output("""if (true) { print("yes"); }"""))
    @Test fun eval_if_notTaken() = assertEquals("", output("""if (false) { print("no"); }"""))
    @Test fun eval_ifElse() = assertEquals("b", output("""if (false) { print("a"); } else { print("b"); }"""))
    @Test fun eval_elseIf() = assertEquals("two", output("""
        int x = 2;
        if (x == 1) { print("one"); }
        else if (x == 2) { print("two"); }
        else { print("other"); }
    """.trimIndent()))

    @Test fun eval_while_sumsToFifteen() = assertEquals("15", output("""
        int sum = 0; int i = 1;
        while (i <= 5) { sum += i; i++; }
        print(to_string(sum));
    """.trimIndent()))

    @Test fun eval_repeat_until() = assertEquals("3", output("""
        int x = 0;
        repeat { x++; } until (x >= 3);
        print(to_string(x));
    """.trimIndent()))

    @Test fun eval_for_ascending() = assertEquals("12345", output("""
        string s = "";
        for i from 1 to 5 { s += to_string(i); }
        print(s);
    """.trimIndent()))

    @Test fun eval_for_downto() = assertEquals("54321", output("""
        string s = "";
        for i from 5 downto 1 { s += to_string(i); }
        print(s);
    """.trimIndent()))

    @Test fun eval_break_exits_loop() = assertEquals("3", output("""
        int x = 0;
        while (true) { x++; if (x == 3) { break; } }
        print(to_string(x));
    """.trimIndent()))

    @Test fun eval_continue_skips_iteration() = assertEquals("135", output("""
        string s = "";
        for i from 1 to 5 {
            if (i == 2 || i == 4) { continue; }
            s += to_string(i);
        }
        print(s);
    """.trimIndent()))

    @Test fun eval_ternary() {
        assertEquals("yes", output("""print(true ? "yes" : "no");"""))
        assertEquals("no", output("""print(false ? "yes" : "no");"""))
    }

    // --- User-defined functions ---

    @Test fun eval_userFunction_add() = assertEquals("7", output("""
        int add(int a, int b) { return a + b; }
        print(to_string(add(3, 4)));
    """.trimIndent()))

    @Test fun eval_userFunction_recursive_factorial() = assertEquals("120", output("""
        int fact(int n) { if (n <= 1) { return 1; } return n * fact(n - 1); }
        print(to_string(fact(5)));
    """.trimIndent()))

    @Test fun eval_userFunction_void_noReturn() = assertEquals("called", output("""
        void greet() { print("called"); }
        greet();
    """.trimIndent()))

    // --- Aggregates ---

    @Test fun eval_aggregate_setGet() = assertEquals("hello", output("""
        string[int] m;
        m[0] = "hello";
        print(m[0]);
    """.trimIndent()))

    @Test fun eval_foreach_over_aggregate() = assertEquals("abc", output("""
        string[int] m;
        m[0] = "a"; m[1] = "b"; m[2] = "c";
        string s = "";
        foreach k, v in m { s += v; }
        print(s);
    """.trimIndent()))

    // --- Records ---

    @Test fun eval_record_fieldAccess() = assertEquals("100", output("""
        record Stats { int hp; int mp; }
        Stats s;
        s.hp = 100;
        print(to_string(s.hp));
    """.trimIndent()))

    // --- Try/catch ---

    @Test fun eval_tryCatch_catchesScriptException() = assertEquals("caught", output("""
        try { int x = 1 / 0; } catch { print("caught"); }
    """.trimIndent()))

    @Test fun eval_try_noException_doesNotRunCatch() = assertEquals("ok", output("""
        try { print("ok"); } catch { print("caught"); }
    """.trimIndent()))
}
