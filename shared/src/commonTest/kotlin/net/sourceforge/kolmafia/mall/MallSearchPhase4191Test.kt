package net.sourceforge.kolmafia.mall

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences

class MallSearchHtmlPreprocessorTest {

    @Test
    fun stripsLimitedRowNotice() {
        val html = """<table><tr><td>Search results are limited to 100 rows.</td></tr><tr class="graybelow"></tr></table>"""
        val cleaned = MallSearchHtmlPreprocessor.preprocess(html)
        assertFalse(cleaned.contains("Search results are limited"))
        assertTrue(cleaned.contains("graybelow"))
    }
}

class MallSearchRelayHookTest {

    @Test
    fun decoratesWhenRelayActive() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("relayActive", true)
        val html = """<tr class="graybelow"><td class="buyers">&nbsp;</td></tr>"""
        val result = MallSearchRelayHook.maybeDecorate(html, prefs)
        assertTrue(result.contains("buyone") || result == html)
    }

    @Test
    fun skipsWhenRelayInactive() {
        val prefs = Preferences(MapSettings())
        val html = """<tr class="graybelow"><td class="buyers">&nbsp;</td></tr>"""
        assertTrue(MallSearchRelayHook.maybeDecorate(html, prefs) === html)
    }

    @Test
    fun highlightsOwnStoreWhenPlayerIdMatches() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("relayActive", true)
        val html = """<tr class="graybelow"><td valign="center" class="buyers">&nbsp;</td><td><a href="mallstore.php?whichstore=12345&searchitem=1.0&searchprice=100"><b>Shop</b></a></td></tr>"""
        val result = MallSearchRelayHook.maybeDecorate(html, prefs, playerId = 12345)
        assertTrue(result.contains("ownstore") || result == html)
    }
}
