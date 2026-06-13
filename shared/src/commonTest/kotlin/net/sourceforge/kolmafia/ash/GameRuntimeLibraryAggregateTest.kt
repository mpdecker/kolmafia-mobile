package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAggregateTest {

    @Test
    fun joinString_joinsStringAggregate() {
        val lib = GameRuntimeLibrary.forTesting()
        val src = """
            string[int] a;
            a[0] = "x"; a[1] = "y";
            print(join_string(a, ","));
        """
        assertEquals("x,y", outputLib(lib, src).trim())
    }

    @Test
    fun containsKey_detectsExistingItemKey() {
        val lib = GameRuntimeLibrary.forTesting()
        val src = """
            int[item] m;
            m[to_item("seal tooth")] = 1;
            print(to_string(contains_key(m, to_item("seal tooth"))));
        """
        assertEquals("true", outputLib(lib, src).trim())
    }

    @Test
    fun containsKey_falseForMissingIntKey() {
        val lib = GameRuntimeLibrary.forTesting()
        val src = """
            int[int] m;
            m[1] = 2;
            print(to_string(contains_key(m, 99)));
        """
        assertEquals("false", outputLib(lib, src).trim())
    }

    @Test
    fun remove_dropsKeyFromAggregate() {
        val lib = GameRuntimeLibrary.forTesting()
        val src = """
            int[int] m;
            m[1] = 2;
            remove(m, 1);
            print(to_string(count(m)));
        """
        assertEquals("0", outputLib(lib, src).trim())
    }

    @Test
    fun keys_returnsIndexAggregate() {
        val lib = GameRuntimeLibrary.forTesting()
        val src = """
            int[int] m;
            m[3] = 9;
            print(to_string(count(keys(m))));
        """
        assertEquals("1", outputLib(lib, src).trim())
    }

    @Test
    fun values_returnsValueAggregate() {
        val lib = GameRuntimeLibrary.forTesting()
        val src = """
            int[int] m;
            m[0] = 7; m[1] = 8;
            print(to_string(count(values(m))));
        """
        assertEquals("2", outputLib(lib, src).trim())
    }
}
