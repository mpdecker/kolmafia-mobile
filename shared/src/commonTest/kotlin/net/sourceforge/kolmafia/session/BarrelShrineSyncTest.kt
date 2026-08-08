package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class BarrelShrineSyncTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun syncFromVisit_alreadyPrayedToday_setsBarrelPrayer() {
        val prefs = prefs()
        BarrelShrineSync.syncFromVisit(
            "You already prayed to the Barrel god today. Come back tomorrow.",
            prefs,
        )
        assertTrue(prefs.getBoolean("_barrelPrayer", false))
    }

    @Test
    fun syncFromVisit_missingProtectionOption_setsPrayedForProtection() {
        val prefs = prefs()
        BarrelShrineSync.syncFromVisit(
            """
            <html>
            Pray for glamour: barrel hoop earring
            Pray for vigor: bankruptcy barrel
            </html>
            """.trimIndent(),
            prefs,
        )
        assertTrue(prefs.getBoolean("prayedForProtection", false))
        assertFalse(prefs.getBoolean("prayedForGlamour", false))
        assertFalse(prefs.getBoolean("prayedForVigor", false))
    }

    @Test
    fun syncFromVisit_allOptionsPresent_leavesAscensionPrefsFalse() {
        val prefs = prefs()
        BarrelShrineSync.syncFromVisit(
            """
            <html>
            barrel lid shield
            barrel hoop earring
            bankruptcy barrel
            </html>
            """.trimIndent(),
            prefs,
        )
        assertFalse(prefs.getBoolean("prayedForProtection", false))
        assertFalse(prefs.getBoolean("prayedForGlamour", false))
        assertFalse(prefs.getBoolean("prayedForVigor", false))
    }

    @Test
    fun syncPostChoice_setsDailyPrayerForOptionsOneThroughFour() {
        val prefs = prefs()
        BarrelShrineSync.syncPostChoice(3, prefs)
        assertTrue(prefs.getBoolean("_barrelPrayer", false))
    }

    @Test
    fun syncPostChoice_optionFive_doesNotSetDailyPrayer() {
        val prefs = prefs()
        BarrelShrineSync.syncPostChoice(5, prefs)
        assertFalse(prefs.getBoolean("_barrelPrayer", false))
    }

    @Test
    fun syncUnlockFromHtml_setsBarrelShrineUnlocked() {
        val prefs = prefs()
        BarrelShrineSync.syncUnlockFromHtml("You install the barrelshrine in your dungeon.", prefs)
        assertTrue(prefs.getBoolean("barrelShrineUnlocked", false))
    }
}
