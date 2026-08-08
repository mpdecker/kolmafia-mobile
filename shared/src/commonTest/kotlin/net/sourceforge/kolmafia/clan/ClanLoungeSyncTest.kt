package net.sourceforge.kolmafia.clan

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionRefreshContext
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.preferences.Preferences

class ClanLoungeSyncTest {

    private val loungeUrl = "https://www.kingdomofloathing.com/clan_viplounge.php"

    @AfterTest
    fun tearDown() {
        SpeakeasyAvailability.resetForTest()
        HotDogAvailability.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun syncFromHtml_setsFloundryPrefWhenImagePresent() {
        val prefs = Preferences(MapSettings())
        ClanLoungeSync.syncFromHtml(
            """<img src="vipfloundry.gif">""",
            prefs,
        )
        assertTrue(prefs.getBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, false))
    }

    @Test
    fun syncFromHtml_setsPhotoBoothPrefWhenImagePresent() {
        val prefs = Preferences(MapSettings())
        ClanLoungeSync.syncFromHtml(
            """<img src="photobooth.gif">""",
            prefs,
        )
        assertTrue(prefs.getBoolean(ClanLoungeSync.CLAN_HAS_PHOTO_BOOTH_PREF, false))
    }

    @Test
    fun syncFromHtml_clearsFloundryPrefWhenImageAbsent() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true)
        ClanLoungeSync.syncFromHtml("<html></html>", prefs)
        assertFalse(prefs.getBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true))
    }

    @Test
    fun apply_onlyRunsForClanLoungeUrl() {
        val prefs = Preferences(MapSettings())
        ClanLoungeSync.apply(prefs, """<img src="vipfloundry.gif">""", "campground.php")
        assertFalse(prefs.getBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, false))
        ClanLoungeSync.apply(
            prefs,
            """<img src="vipfloundry.gif">""",
            loungeUrl,
        )
        assertTrue(prefs.getBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, false))
    }

    @Test
    fun syncSpeakeasyDrinksDrunkFromHtml_setsDrinkCountFromVisitText() {
        val prefs = Preferences(MapSettings())
        ClanLoungeSync.syncSpeakeasyDrinksDrunkFromHtml("You have 3 more drinks today.", prefs)
        assertEquals(0, prefs.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF))
        ClanLoungeSync.syncSpeakeasyDrinksDrunkFromHtml("You have 2 more drinks today.", prefs)
        assertEquals(1, prefs.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF))
        ClanLoungeSync.syncSpeakeasyDrinksDrunkFromHtml("You have one more drink today.", prefs)
        assertEquals(2, prefs.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF))
        ClanLoungeSync.syncSpeakeasyDrinksDrunkFromHtml("You've had your limit.", prefs)
        assertEquals(3, prefs.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF))
    }

    @Test
    fun apply_speakeasyVisitSyncsDrinksDrunkPref() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF, 99)
        ClanLoungeSync.apply(
            prefs,
            """<p>You have 2 more drinks today.</p>""",
            "$loungeUrl?action=speakeasy",
        )
        assertEquals(1, prefs.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF))
    }

    @Test
    fun syncHotDogEatFromResponse_setsFancyPrefOnFancySuccess() {
        val prefs = Preferences(MapSettings())
        ClanLoungeSync.syncHotDogEatFromResponse(
            html = "You gain some stats.",
            url = "$loungeUrl?preaction=eathotdog&whichdog=-95",
            prefs = prefs,
        )
        assertTrue(prefs.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false))
    }

    @Test
    fun syncHotDogEatFromResponse_setsFancyPrefOnDailyLimitMessage() {
        val prefs = Preferences(MapSettings())
        ClanLoungeSync.syncHotDogEatFromResponse(
            html = "You aren't in the mood for any more fancy dogs today.",
            url = "$loungeUrl?preaction=eathotdog&whichdog=-95",
            prefs = prefs,
        )
        assertTrue(prefs.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false))
    }

    @Test
    fun syncHotDogEatFromResponse_basicDogDoesNotSetFancyPref() {
        val prefs = Preferences(MapSettings())
        ClanLoungeSync.syncHotDogEatFromResponse(
            html = "You gain some stats.",
            url = "$loungeUrl?preaction=eathotdog&whichdog=-92",
            prefs = prefs,
        )
        assertFalse(prefs.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false))
    }

    @Test
    fun syncHotDogEatFromResponse_tooFullDoesNotSetFancyPref() {
        val prefs = Preferences(MapSettings())
        ClanLoungeSync.syncHotDogEatFromResponse(
            html = "You're too full to eat that.",
            url = "$loungeUrl?preaction=eathotdog&whichdog=-95",
            prefs = prefs,
        )
        assertFalse(prefs.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false))
    }

    @Test
    fun syncSpeakeasyDrinkFromResponse_incrementsDrinksDrunkOnSuccess() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF, 1)
        ClanLoungeSync.syncSpeakeasyDrinkFromResponse(
            html = "You drink a Lucky Lindy.",
            url = "$loungeUrl?preaction=speakeasydrink&drink=4",
            prefs = prefs,
        )
        assertEquals(2, prefs.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF))
    }

    @Test
    fun syncSpeakeasyDrinkFromResponse_failureDoesNotIncrement() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF, 1)
        ClanLoungeSync.syncSpeakeasyDrinkFromResponse(
            html = "You can't afford that.",
            url = "$loungeUrl?preaction=speakeasydrink&drink=4",
            prefs = prefs,
        )
        assertEquals(1, prefs.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF))
    }

    @Test
    fun syncFromHtml_setsHotDogStandAndSpeakeasyFurniturePrefs() {
        val prefs = Preferences(MapSettings())
        ClanLoungeSync.syncFromHtml(
            """<img src="hotdogstand.gif"><img src="speakeasy.gif">""",
            prefs,
        )
        assertTrue(prefs.getBoolean(ClanLoungeSync.CLAN_HAS_HOT_DOG_STAND_PREF, false))
        assertTrue(prefs.getBoolean(ClanLoungeSync.CLAN_HAS_SPEAKEASY_PREF, false))
    }

    @Test
    fun apply_speakeasyVisitResetsAndRebuildsAvailability() {
        SpeakeasyAvailability.addLoungeId(1)
        val prefs = Preferences(MapSettings())
        val html = """
            <p>You have 3 more drinks today.</p>
            <input name="drink" value="4">
            <input name="drink" value="5">
        """.trimIndent()
        ClanLoungeSync.apply(prefs, html, "$loungeUrl?action=speakeasy")

        assertFalse(SpeakeasyAvailability.isAvailable("glass of &quot;milk&quot;"))
        assertTrue(SpeakeasyAvailability.isAvailable("Lucky Lindy"))
        assertTrue(SpeakeasyAvailability.isAvailable("Bee's Knees"))
        assertEquals(0, prefs.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF))
    }

    @Test
    fun apply_hotDogStandVisitParsesAvailabilityAndRefreshesRuntime() {
        val prefs = Preferences(MapSettings())
        val html = """
            <table><tr><form action=clan_viplounge.php method=post>
            <tr><td><input class=button type=submit value=Eat><span onclick='descitem."-92_food"'><b>basic hot dog</b></span></td></tr>
            <tr><td><input class=button type=submit value=Eat disabled><span onclick='descitem."-95_food"'><b>sly dog</b></span></td></tr>
            </form></table>
        """.trimIndent()
        ClanLoungeSync.apply(prefs, html, "$loungeUrl?action=hotdogstand")

        assertTrue(HotDogAvailability.isAvailable("basic hot dog"))
        assertFalse(HotDogAvailability.isAvailable("sly dog"))
        assertEquals(1, ConcoctionDatabase.totalCount("basic hot dog"))
        assertEquals(0, ConcoctionDatabase.totalCount("sly dog"))
    }

    @Test
    fun apply_eathotdogFancySuccessRefreshesRuntimeToZero() {
        HotDogAvailability.addForTest("sly dog")
        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.EMPTY)
        assertEquals(1, ConcoctionDatabase.totalCount("sly dog"))

        val prefs = Preferences(MapSettings())
        ClanLoungeSync.apply(
            prefs,
            html = "You gain some stats.",
            url = "$loungeUrl?preaction=eathotdog&whichdog=-95",
        )

        assertTrue(prefs.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false))
        assertEquals(0, ConcoctionDatabase.totalCount("sly dog"))
    }

    @Test
    fun apply_unrelatedLoungePageLeavesDailyLimitPrefsUnchanged() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false)
        prefs.setInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF, 2)
        ClanLoungeSync.apply(
            prefs,
            """<img src="vipfloundry.gif">""",
            "$loungeUrl?action=fireworks",
        )
        assertFalse(prefs.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, true))
        assertEquals(2, prefs.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF))
    }
}
