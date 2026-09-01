package net.sourceforge.kolmafia.combat

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.banish.Banisher

class FightBanishSyncTest {

    @Test
    fun resolvesRightZootKickFallback() {
        val html = "You deliver a Right Zoot Kick and your opponent heads off into the distance."
        assertEquals(
            Banisher.RIGHT_ZOOT_KICK,
            FightBanishSync.resolveBanisher(html, Banisher.UNKNOWN),
        )
    }

    @Test
    fun resolvesBlartFallback() {
        val html = "You spray B. L. A. R. T. everywhere and your foe flees."
        assertEquals(
            Banisher.BLART_SPRAY_WIDE,
            FightBanishSync.resolveBanisher(html, Banisher.UNKNOWN),
        )
    }
}
