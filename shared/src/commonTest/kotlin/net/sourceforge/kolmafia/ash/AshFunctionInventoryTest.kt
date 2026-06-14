package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertTrue

class AshFunctionInventoryTest {

    @Test
    fun registeredOverloadCount_meetsAshP8Floor() {
        val scope = AshScope()
        GameRuntimeLibrary.forTesting().registerAll(scope)
        val count = scope.debugFunctionCount()
        assertTrue(count >= 326, "Expected ≥326 overloads after ASH-P8, got $count")
    }
}
