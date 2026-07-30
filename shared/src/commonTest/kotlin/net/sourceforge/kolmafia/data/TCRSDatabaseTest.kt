package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings

class TCRSDatabaseTest {

    @AfterTest
    fun tearDown() {
        TCRSDatabase.reset()
    }

    @Test
    fun parseFromText_readsTabSeparatedRows() {
        val text = buildString {
            appendLine("471\tbouncing spicy batwing\t1\t\tEffect: \"Spicy\", Effect Duration: 5")
            appendLine("1\tmirror seal-clubbing club\t0\t\t")
            appendLine("bad-row")
        }
        val map = TCRSDatabase.parseFromText(text)
        assertEquals(2, map.size)
        assertEquals("bouncing spicy batwing", map[471]?.name)
        assertEquals(1, map[471]?.size)
        assertEquals("mirror seal-clubbing club", map[1]?.name)
    }

    @Test
    fun filename_matchesDesktopPattern() {
        assertEquals(
            "TCRS_Seal_Clubber_Mongoose.txt",
            TCRSDatabase.filename("Seal Clubber", "Mongoose"),
        )
        assertEquals(
            "TCRS_Seal_Clubber_Mongoose_cafe_booze.txt",
            TCRSDatabase.filename("Seal Clubber", "Mongoose", "_cafe_booze"),
        )
    }

    @Test
    fun validate_requiresStandardClassAndSign() {
        assertTrue(TCRSDatabase.validate("Seal Clubber", "Mongoose"))
        assertFalse(TCRSDatabase.validate("Ed the Undying", "Mongoose"))
        assertFalse(TCRSDatabase.validate("Seal Clubber", "Bad Moon"))
        assertFalse(TCRSDatabase.validate("Seal Clubber", "Unknown"))
    }

    @Test
    fun getTCRSName_fallsBackToItemDatabaseName() {
        assertEquals("", TCRSDatabase.getTCRSName(999999))
    }

    @Test
    fun prefRoundTrip_persistsLoadedMap() {
        val prefs = Preferences(MapSettings())
        val fixture = "471\tbouncing spicy batwing\t1\t\t"
        TCRSDatabase.load("Seal Clubber", "Mongoose", fixture)
        assertTrue(TCRSDatabase.saveToPreferences("Seal Clubber", "Mongoose", prefs))
        TCRSDatabase.reset()
        assertEquals(0, TCRSDatabase.mapSizeForTest())
        assertTrue(TCRSDatabase.loadFromPreferences("Seal Clubber", "Mongoose", prefs))
        assertEquals("bouncing spicy batwing", TCRSDatabase.getTCRSName(471))
        assertEquals("Seal Clubber/Mongoose", TCRSDatabase.currentClassSignForTest())
    }
}
