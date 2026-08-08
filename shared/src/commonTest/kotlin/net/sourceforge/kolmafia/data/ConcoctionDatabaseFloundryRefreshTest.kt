package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.preferences.Preferences

class ConcoctionDatabaseFloundryRefreshTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        FloundryAvailability.resetForTest()
    }

    @Test
    fun refresh_availableCarpe_setsInitialFromStock() {
        FloundryAvailability.addForTest("carpe", 123)
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true)
        val context = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = prefs,
        )

        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(12, ConcoctionDatabase.initialCount("carpe"))
        assertEquals(12, ConcoctionDatabase.totalCount("carpe"))
        assertEquals(12, ConcoctionDatabase.creatableCount("carpe"))
    }

    @Test
    fun refresh_unavailableFish_setsInitialZero() {
        FloundryAvailability.addForTest("carpe", 5)
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true)

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext(
                characterState = CharacterState(),
                preferences = prefs,
            ),
        )

        assertEquals(0, ConcoctionDatabase.initialCount("carpe"))
        assertEquals(0, ConcoctionDatabase.totalCount("carpe"))
    }

    @Test
    fun refresh_dailyUsed_setsInitialZeroEvenWhenStockAvailable() {
        FloundryAvailability.addForTest("carpe", 100)
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true)
        prefs.setBoolean(ClanLoungeSync.FLOUNDRY_ITEM_USED_PREF, true)

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext(
                characterState = CharacterState(),
                preferences = prefs,
            ),
        )

        assertEquals(0, ConcoctionDatabase.initialCount("carpe"))
    }

    @Test
    fun refresh_registersVirtualFloundryConcoction() {
        FloundryAvailability.addForTest("carpe", 100)

        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.EMPTY)

        val concoction = ConcoctionDatabase.getByResult("carpe")
        assertNotNull(concoction)
        assertTrue(concoction.methods.contains("FLOUNDRY"))
    }
}
