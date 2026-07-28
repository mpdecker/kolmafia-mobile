package net.sourceforge.kolmafia.clan

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class ClanLoungeSyncTest {

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
            "https://www.kingdomofloathing.com/clan_viplounge.php",
        )
        assertTrue(prefs.getBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, false))
    }
}
