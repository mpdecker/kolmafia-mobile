package net.sourceforge.kolmafia.clan

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionRefreshContext
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConsumableQuality
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.preferences.Preferences

class ClanHotdogMenuCacheTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        HotDogAvailability.resetForTest()
    }

    @Test
    fun saveAndRestore_repopulatesAvailabilityAndRuntime() {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "basic hot dog",
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.CRAPPY,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.CLAN_HAS_HOT_DOG_STAND_PREF, true)
        HotDogAvailability.addForTest("basic hot dog")
        ClanHotdogMenuCache.saveMenu(prefs)
        HotDogAvailability.resetForTest()

        ClanHotdogMenuCache.restoreIntoAvailability(prefs)

        assertTrue(HotDogAvailability.isAvailable("basic hot dog"))
        assertEquals(1, ConcoctionDatabase.totalCount("basic hot dog"))
    }

    @Test
    fun apply_hotDogStandVisitWritesMenuPref() {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "basic hot dog",
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.CRAPPY,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        val prefs = Preferences(MapSettings())
        val html = """
            <table><tr><form action=clan_viplounge.php method=post>
            <tr><td><input class=button type=submit value=Eat><span><b>basic hot dog</b></span></td></tr>
            </form></table>
        """.trimIndent()
        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.EMPTY)

        ClanLoungeSync.apply(prefs, html, "https://www.kingdomofloathing.com/clan_viplounge.php?action=hotdogstand")

        assertTrue(
            prefs.getString(ClanHotdogMenuCache.CACHED_HOT_DOG_STAND_MENU_PREF, "")
                .contains("basic hot dog"),
        )
    }
}
