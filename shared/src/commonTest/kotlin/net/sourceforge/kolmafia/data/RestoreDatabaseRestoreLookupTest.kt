package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.modifiers.ExpressionContext

class RestoreDatabaseRestoreLookupTest {

    @BeforeTest
    fun setUp() = runTest {
        RestoreDatabase.load()
    }

    @AfterTest
    fun tearDown() {
        RestoreDatabase.resetForTest()
    }

    @Test
    fun getHpMinMax_numericAspirin() {
        val ctx = ExpressionContext.EMPTY
        assertEquals(101, RestoreDatabase.getHpMinByName("aspirin", ctx))
        assertEquals(101, RestoreDatabase.getHpMaxByName("aspirin", ctx))
    }

    @Test
    fun getMpMinMax_numericRange() {
        val ctx = ExpressionContext.EMPTY
        assertEquals(30, RestoreDatabase.getMpMinByName("ancient pills", ctx))
        assertEquals(40, RestoreDatabase.getMpMaxByName("ancient pills", ctx))
    }

    @Test
    fun getMpMinMax_pathExpression_offYouRobotPath() {
        val ctx = ExpressionContext(challengePath = "")
        assertEquals(30, RestoreDatabase.getMpMinByName("battery (AAA)", ctx))
        assertEquals(30, RestoreDatabase.getMpMaxByName("battery (AAA)", ctx))
    }

    @Test
    fun getMpMinMax_pathExpression_onYouRobotPath() {
        val ctx = ExpressionContext(challengePath = AscensionPath.YOU_ROBOT.apiName)
        assertEquals(0, RestoreDatabase.getMpMinByName("battery (AAA)", ctx))
        assertEquals(0, RestoreDatabase.getMpMaxByName("battery (AAA)", ctx))
    }

    @Test
    fun getHpMinMax_fullRestoreExpression() {
        val ctx = ExpressionContext(characterMaxHp = 500)
        assertEquals(500, RestoreDatabase.getHpMinByName("goodberry", ctx))
        assertEquals(500, RestoreDatabase.getHpMaxByName("goodberry", ctx))
    }

    @Test
    fun getHpMinMax_unknownItem_returnsZero() {
        val ctx = ExpressionContext.EMPTY
        assertEquals(0, RestoreDatabase.getHpMinByName("no such restore item", ctx))
        assertEquals(0, RestoreDatabase.getMpMaxByName("no such restore item", ctx))
    }

    @Test
    fun pathSafeHp_vampyreBlocksHpRestore() {
        val ctx = ExpressionContext(challengePath = AscensionPath.VAMPYRE.apiName)
        assertEquals(0, RestoreDatabase.getHpMinByName("aspirin", ctx))
    }

    @Test
    fun evalRestoreValue_plainNumeric() {
        assertEquals(101.0, RestoreDatabase.evalRestoreValue("101", "aspirin", ExpressionContext.EMPTY))
    }
}
