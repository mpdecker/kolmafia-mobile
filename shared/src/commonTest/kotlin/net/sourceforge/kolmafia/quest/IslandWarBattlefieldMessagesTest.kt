package net.sourceforge.kolmafia.quest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IslandWarBattlefieldMessagesTest {

    @Test
    fun victoryMessage_singularFratSide() {
        assertEquals(
            "1 frat boy defeated; 64 down, 936 left.",
            IslandWarBattlefieldMessages.victoryMessage(
                defeatingFratSide = true,
                last = 63,
                current = 64,
                isKingdomOfExploathing = false,
            ),
        )
    }

    @Test
    fun victoryMessage_pluralHippySide() {
        assertEquals(
            "4 hippies defeated; 10 down, 990 left.",
            IslandWarBattlefieldMessages.victoryMessage(
                defeatingFratSide = false,
                last = 6,
                current = 10,
                isKingdomOfExploathing = false,
            ),
        )
    }

    @Test
    fun victoryMessage_koeTotal333() {
        assertEquals(
            "1 hippy defeated; 100 down, 233 left.",
            IslandWarBattlefieldMessages.victoryMessage(
                defeatingFratSide = false,
                last = 99,
                current = 100,
                isKingdomOfExploathing = true,
            ),
        )
    }

    @Test
    fun areaMessage_thresholdCrossAt64_lighthouseOnFratSide() {
        assertEquals(
            "The Lighthouse is now accessible in this uniform!",
            IslandWarBattlefieldMessages.areaMessage(
                defeatingFratSide = true,
                last = 63,
                current = 64,
                isKingdomOfExploathing = false,
            ),
        )
    }

    @Test
    fun areaMessage_noCross_returnsNull() {
        assertNull(
            IslandWarBattlefieldMessages.areaMessage(
                defeatingFratSide = true,
                last = 64,
                current = 65,
                isKingdomOfExploathing = false,
            ),
        )
    }

    @Test
    fun areaMessage_koeAlwaysNull() {
        assertNull(
            IslandWarBattlefieldMessages.areaMessage(
                defeatingFratSide = true,
                last = 63,
                current = 64,
                isKingdomOfExploathing = true,
            ),
        )
    }

    @Test
    fun heroMessage_thresholdCrossAt501() {
        assertEquals(
            "Keep your eyes open for the Next-Generation Frat Boy!",
            IslandWarBattlefieldMessages.heroMessage(
                defeatingFratSide = true,
                last = 500,
                current = 501,
                isKingdomOfExploathing = false,
            ),
        )
    }

    @Test
    fun heroMessage_koeAlwaysNull() {
        assertNull(
            IslandWarBattlefieldMessages.heroMessage(
                defeatingFratSide = false,
                last = 500,
                current = 501,
                isKingdomOfExploathing = true,
            ),
        )
    }

    @Test
    fun finishWarMessage_hippiesDefeated() {
        assertEquals(
            "War finished: hippies defeated",
            IslandWarBattlefieldMessages.finishWarMessage("hippies"),
        )
    }
}
