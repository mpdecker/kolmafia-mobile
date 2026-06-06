// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/banish/BanishManagerTest.kt
package net.sourceforge.kolmafia.banish

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class BanishManagerTest {

    // ── Banisher.fromName ────────────────────────────────────────────────────

    @Test fun banisher_fromName_knownBanisher_returnsIt() {
        assertEquals(Banisher.SNOKEBOMB, Banisher.fromName("snokebomb"))
    }

    @Test fun banisher_fromName_caseInsensitive() {
        assertEquals(Banisher.SABER_FORCE, Banisher.fromName("Saber Force"))
    }

    @Test fun banisher_fromName_unknownName_returnsUnknown() {
        assertEquals(Banisher.UNKNOWN, Banisher.fromName("some imaginary banisher"))
    }

    // ── BanishedMonster.isExpired ────────────────────────────────────────────

    @Test fun isExpired_turnBased_withinTurns_returnsFalse() {
        val b = BanishedMonster("Foo", Banisher.SNOKEBOMB, turnBanished = 100)
        // SNOKEBOMB has 30 turns; at turn 129 it's still active
        assertFalse(b.isExpired(currentTurn = 129))
    }

    @Test fun isExpired_turnBased_atExpiryTurn_returnsTrue() {
        val b = BanishedMonster("Foo", Banisher.SNOKEBOMB, turnBanished = 100)
        // 100 + 30 = 130; at turn 130 it expires
        assertTrue(b.isExpired(currentTurn = 130))
    }

    @Test fun isExpired_rolloverBanish_neverExpiresWithinRun() {
        val b = BanishedMonster("Foo", Banisher.BEANCANNON, turnBanished = 1)
        // BEANCANNON is ROLLOVER; never expires mid-run
        assertFalse(b.isExpired(currentTurn = 999))
    }

    @Test fun isExpired_neverReset_alwaysFalse() {
        val b = BanishedMonster("Foo", Banisher.ICE_HOUSE, turnBanished = 0)
        assertFalse(b.isExpired(currentTurn = 9999))
    }

    // BanishManager tests continue in Task 6
}
