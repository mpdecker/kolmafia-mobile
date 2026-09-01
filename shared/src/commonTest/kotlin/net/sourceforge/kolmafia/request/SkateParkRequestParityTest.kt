package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class SkateParkRequestParityTest {
    @Test
    fun responseParserUpdatesWarStateAndDailyUse() {
        val prefs = Preferences(MapSettings())
        val html = """
            <img src="ocean/ice_territory.gif">
            You acquire an effect: <b>Ice Cold</b>
        """.trimIndent()
        assertFalse(SkateParkRequest.parseResponse("state2buff1", html, prefs))
        assertEquals("ice", prefs.getString("skateParkStatus"))
        assertTrue(prefs.getBoolean("_skateBuff1"))
    }

    @Test
    fun resetClearsPriorBuffsOnNewAscension() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastSkateParkReset", 4)
        prefs.setBoolean("_skateBuff1", true)
        prefs.setString("skateParkStatus", "peace")

        SkateParkRequest.ensureUpdatedSkatePark(prefs, 5)

        assertEquals(5, prefs.getInt("lastSkateParkReset"))
        assertEquals("war", prefs.getString("skateParkStatus"))
        assertFalse(prefs.getBoolean("_skateBuff1"))
    }

    @Test
    fun invalidActionResponseDoesNotClaimBuff() {
        val prefs = Preferences(MapSettings())
        val failed = SkateParkRequest.parseResponse(
            "state2buff1",
            "You've already dined with Lutz.",
            prefs,
        )
        assertTrue(failed)
        assertTrue(prefs.getBoolean("_skateBuff1"))
    }
}
