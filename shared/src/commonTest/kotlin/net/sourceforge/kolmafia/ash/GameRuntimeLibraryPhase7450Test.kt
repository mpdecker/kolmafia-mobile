package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.request.RichardRequest
import net.sourceforge.kolmafia.session.HobopolisManager
import net.sourceforge.kolmafia.session.SlimeStackManager
import net.sourceforge.kolmafia.session.SlimeTubeManager

/** Clan dungeon residual mega wrap (phases 7391–7450). */
class GameRuntimeLibraryPhase7450Test {

    @Test
    fun revision_isPhase7450() {
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
        assertEquals("7510", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun clanDungeonResidualsAreLive() {
        assertEquals("Hodgman", HobopolisManager.hobopolisBossName(200))
        assertEquals("Mother Slime waits for you.", SlimeTubeManager.motherSlimeWait(326, 2))
        assertEquals(6, SlimeStackManager.getSlimeStackTurns(3))
        assertEquals(
            "Help Richard make grenades (Moxie)",
            RichardRequest.gymType("clan_hobopolis.php?place=3&preaction=spendturns&whichservice=2"),
        )
    }
}
