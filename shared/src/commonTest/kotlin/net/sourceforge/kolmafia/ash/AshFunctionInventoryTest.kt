package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertTrue

class AshFunctionInventoryTest {

    @Test
    fun registeredOverloadCount_meetsAshP5Floor() {
        val scope = AshScope()
        GameRuntimeLibrary.forTesting().registerAll(scope)
        val count = scope.debugFunctionCount()
        assertTrue(count >= 300, "Expected ≥300 overloads after ASH-P5, got $count")
    }
}
