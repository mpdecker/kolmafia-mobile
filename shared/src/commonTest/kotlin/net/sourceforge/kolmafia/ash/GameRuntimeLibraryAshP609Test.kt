package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.session.ElVibratoManager

class GameRuntimeLibraryAshP609Test {

    @Test
    fun revision_phase609() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun punchcards_includeAttackAndSphere() {
        assertEquals(11, ElVibratoManager.PUNCHCARDS.size)
        assertEquals(3146, ElVibratoManager.ATTACK.id)
        assertEquals("ATTACK", ElVibratoManager.ATTACK.tag)
        assertEquals(3156, ElVibratoManager.SPHERE.id)
        assertEquals(ElVibratoManager.TARGET.id, ElVibratoManager.CARD_EXCHANGES[ElVibratoManager.ATTACK.id])
    }

    @Test
    fun lonely_construct_hasSphereModify() {
        val cmds = ElVibratoManager.commandsFor(ElVibratoManager.Construct.LONELY)
        assertTrue(cmds.any { it.card1.tag == "MODIFY" && it.card2.tag == "SPHERE" })
    }

    @Test
    fun parseResponse_consumesWhichcard() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            ElVibratoManager.parseResponse(
                url = "fight.php?action=useitem&whichitem=3146&whichcard=3146",
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(3146 to 1), consumed)
    }
}
