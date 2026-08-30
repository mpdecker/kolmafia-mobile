package net.sourceforge.kolmafia.adventure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FightActionTest {
    @Test
    fun typedActionsBuildDesktopFightFields() {
        assertEquals(mapOf("action" to "attack"), FightAction.attack().formFields())
        assertEquals(
            mapOf("action" to "skill", "whichskill" to "123"),
            FightAction.skill(123).formFields(),
        )
        assertEquals(
            mapOf("action" to "useitem", "whichitem" to "10", "whichitem2" to "11"),
            FightAction.item(10, 11).formFields(),
        )
        assertEquals(
            mapOf("action" to "macro", "macrotext" to "attack"),
            FightAction.macro("attack").formFields(),
        )
    }

    @Test
    fun compactActionsParseSafely() {
        assertEquals(FightActionKind.RUNAWAY, FightAction.parse("runaway")?.kind)
        assertEquals(42, FightAction.parse("skill42")?.skillId)
        assertEquals(FightActionKind.ITEM, FightAction.parse("10,11")?.kind)
        assertNull(FightAction.parse("not-an-action"))
    }

    @Test
    fun lifecycleTracksMultiFightRoundsAndClears() {
        val lifecycle = FightLifecycle()
        lifecycle.beginFight()
        lifecycle.beginRound(FightAction.attack())
        assertEquals(1, lifecycle.context.round)
        lifecycle.recordResponse("<p>You hit it.</p><p>fight.php?ireallymeanit=1</p>")
        assertEquals(2, lifecycle.context.round)
        assertEquals(FightActionKind.ATTACK, lifecycle.context.action?.kind)
        lifecycle.clear()
        assertEquals(0, lifecycle.context.round)
        assertEquals("", lifecycle.context.lastResponse)
    }
}
