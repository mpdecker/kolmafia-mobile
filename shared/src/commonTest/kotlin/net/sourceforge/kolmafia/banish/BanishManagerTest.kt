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

    // ── BanishManager ────────────────────────────────────────────────────────

    @Test fun banishMonster_recordsEntry() {
        val manager = BanishManager(prefs())
        manager.banishMonster("Ninja Snowman", Banisher.SNOKEBOMB, currentTurn = 50)
        assertTrue(manager.isBanished("Ninja Snowman", currentTurn = 60))
    }

    @Test fun banishMonster_caseInsensitiveLookup() {
        val manager = BanishManager(prefs())
        manager.banishMonster("ninja snowman", Banisher.SNOKEBOMB, currentTurn = 50)
        assertTrue(manager.isBanished("Ninja Snowman", currentTurn = 60))
    }

    @Test fun banishMonster_duplicateMonster_replacesExisting() {
        val manager = BanishManager(prefs())
        manager.banishMonster("Ninja Snowman", Banisher.SNOKEBOMB, currentTurn = 50)
        manager.banishMonster("Ninja Snowman", Banisher.SABER_FORCE, currentTurn = 80)
        // Only one entry; uses newer banish
        assertEquals(1, manager.state.value.monsters.size)
        assertEquals(Banisher.SABER_FORCE, manager.state.value.monsters.first().banisher)
    }

    @Test fun isBanished_unknownMonster_returnsFalse() {
        val manager = BanishManager(prefs())
        assertFalse(manager.isBanished("Random Monster", currentTurn = 1))
    }

    @Test fun isBanished_turnBanish_withinTurns_returnsTrue() {
        val manager = BanishManager(prefs())
        manager.banishMonster("Foo", Banisher.SNOKEBOMB, currentTurn = 100)
        assertTrue(manager.isBanished("Foo", currentTurn = 129))   // 100 + 30 - 1 = 129
    }

    @Test fun isBanished_turnBanish_expired_returnsFalse() {
        val manager = BanishManager(prefs())
        manager.banishMonster("Foo", Banisher.SNOKEBOMB, currentTurn = 100)
        assertFalse(manager.isBanished("Foo", currentTurn = 130))  // 100 + 30 = expired
    }

    @Test fun clearExpiredAndRollover_removesRolloverBanishes() {
        val manager = BanishManager(prefs())
        manager.banishMonster("Foo", Banisher.BEANCANNON, currentTurn = 1)   // ROLLOVER
        manager.clearExpiredAndRollover(currentTurn = 2)
        assertFalse(manager.isBanished("Foo", currentTurn = 2))
    }

    @Test fun clearExpiredAndRollover_removesAvatarBanishes() {
        val manager = BanishManager(prefs())
        manager.banishMonster("Foo", Banisher.BANISHING_SHOUT, currentTurn = 1)  // AVATAR
        manager.clearExpiredAndRollover(currentTurn = 2)
        assertFalse(manager.isBanished("Foo", currentTurn = 2))
    }

    @Test fun clearExpiredAndRollover_removesTurnRolloverBanishes() {
        val manager = BanishManager(prefs())
        manager.banishMonster("Foo", Banisher.SNOKEBOMB, currentTurn = 1)  // TURN_ROLLOVER
        // Regardless of turn count, TURN_ROLLOVER banishes clear on rollover/login
        manager.clearExpiredAndRollover(currentTurn = 5)
        assertFalse(manager.isBanished("Foo", currentTurn = 5))
    }

    @Test fun clearExpiredAndRollover_keepsNeverBanish() {
        val manager = BanishManager(prefs())
        manager.banishMonster("Foo", Banisher.ICE_HOUSE, currentTurn = 1)   // NEVER
        manager.clearExpiredAndRollover(currentTurn = 9999)
        assertTrue(manager.isBanished("Foo", currentTurn = 9999))
    }

    @Test fun save_and_load_roundtrip() {
        val s = com.russhwolf.settings.MapSettings()
        val p = net.sourceforge.kolmafia.preferences.Preferences(s)
        val manager = BanishManager(p)
        manager.banishMonster("Ninja Snowman", Banisher.SNOKEBOMB, currentTurn = 100)
        manager.banishMonster("Ice Cream Sandwich", Banisher.SABER_FORCE, currentTurn = 200)
        manager.save()

        val manager2 = BanishManager(p)
        manager2.load()
        assertTrue(manager2.isBanished("Ninja Snowman", currentTurn = 110))
        assertTrue(manager2.isBanished("Ice Cream Sandwich", currentTurn = 210))
    }

    @Test fun load_emptyPrefs_setsEmptyState() {
        val manager = BanishManager(prefs())
        manager.load()
        assertTrue(manager.state.value.monsters.isEmpty())
    }

    @Test fun load_malformedEntry_skipsIt() {
        val s = com.russhwolf.settings.MapSettings()
        s.putString(net.sourceforge.kolmafia.preferences.Preferences.BANISHED_MONSTERS, "bad|Ninja Snowman:snokebomb:100")
        val p = net.sourceforge.kolmafia.preferences.Preferences(s)
        val manager = BanishManager(p)
        manager.load()
        // "bad" entry has only 1 field; should be skipped; valid entry should load
        assertEquals(1, manager.state.value.monsters.size)
    }

    // ── Test helper ───────────────────────────────────────────────────────────

    private fun prefs(): net.sourceforge.kolmafia.preferences.Preferences =
        net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
}
