package net.sourceforge.kolmafia.clan

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.preferences.Preferences

class ClanManagerTest {

    @AfterTest
    fun tearDown() {
        ClanManager.resetForTest()
        HotDogAvailability.resetForTest()
    }

    @Test
    fun setClan_tracksIdAndName() {
        ClanManager.setClan(42, "Test Clan")
        assertEquals(42, ClanManager.getClanId())
        assertEquals("Test Clan", ClanManager.getClanName())
    }

    @Test
    fun clearCache_resetsClanState() {
        ClanManager.setClan(42, "Test Clan")
        ClanManager.addHotdog("basic hot dog")
        ClanManager.clearCache(newCharacter = true)
        assertEquals(0, ClanManager.getClanId())
        assertEquals("", ClanManager.getClanName())
        assertEquals(emptyList(), ClanManager.getHotdogs())
    }

    @Test
    fun addHotdog_tracksPerClanMenu() {
        ClanManager.setClan(7, "Clan Seven")
        ClanManager.addHotdog("basic hot dog")
        ClanManager.addHotdog("fancy hot dog")
        assertEquals(listOf("basic hot dog", "fancy hot dog"), ClanManager.getHotdogs())
    }

    @Test
    fun hotdogMenuCache_prefersClanManagerWhenClanKnown() {
        ClanManager.setClan(99, "Cache Clan")
        ClanManager.addHotdog("basic hot dog")
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.CLAN_HAS_HOT_DOG_STAND_PREF, true)

        ClanHotdogMenuCache.restoreIntoAvailability(prefs)

        assertEquals(1, HotDogAvailability.snapshotNames().size)
        assertEquals("basic hot dog", HotDogAvailability.snapshotNames().single())
    }
}
