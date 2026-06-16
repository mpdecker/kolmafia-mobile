package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertTrue

class AshFunctionInventoryTest {

    @Test
    fun registeredOverloadCount_meetsAshP11Floor() {
        val scope = AshScope()
        GameRuntimeLibrary.forTesting().registerAll(scope)
        val count = scope.debugFunctionCount()
        assertTrue(count >= 890, "Expected ≥890 overloads after ASH-P15, got $count")
    }
}
