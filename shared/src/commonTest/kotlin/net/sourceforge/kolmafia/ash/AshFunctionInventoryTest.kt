package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertTrue

class AshFunctionInventoryTest {

    @Test
    fun registeredOverloadCount_meetsAshP11Floor() {
        val scope = AshScope()
        GameRuntimeLibrary.forTesting().registerAll(scope)
        val count = scope.debugFunctionCount()
        assertTrue(count >= 820, "Expected ≥820 overloads after ASH-P14, got $count")
    }
}
