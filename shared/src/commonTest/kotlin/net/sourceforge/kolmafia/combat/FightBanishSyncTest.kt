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
    fun resolvesKneecappingFallback() {
        val html = "You call in a favor from your mob and they flee in terror."
        assertEquals(
            Banisher.ORDER_A_KNEECAPPING,
            FightBanishSync.resolveBanisher(html, Banisher.UNKNOWN),
        )
    }

    @Test
    fun resolvesScrapbookBoredomFallback() {
        val html = "They pass out from pure boredom and flee in terror."
        assertEquals(
            Banisher.SHOW_YOUR_BORING_FAMILIAR_PICTURES,
            FightBanishSync.resolveBanisher(html, Banisher.UNKNOWN),
        )
    }
}
