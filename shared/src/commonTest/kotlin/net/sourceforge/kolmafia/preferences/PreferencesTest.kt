package net.sourceforge.kolmafia.preferences

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreferencesTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun getString_returnsDefault_whenKeyNotSet() {
        assertEquals("", prefs().getString("missing"))
    }

    @Test
    fun getString_returnsCustomDefault_whenKeyNotSet() {
        assertEquals("fallback", prefs().getString("missing", "fallback"))
    }

    @Test
    fun setAndGetString_roundTrips() {
        val p = prefs()
        p.setString("key", "value")
        assertEquals("value", p.getString("key"))
    }

    @Test
    fun setAndGetBoolean_roundTrips() {
        val p = prefs()
        p.setBoolean("flag", true)
        assertTrue(p.getBoolean("flag"))
    }

    @Test
    fun getBoolean_returnsFalse_byDefault() {
        assertFalse(prefs().getBoolean("missing"))
    }

    @Test
    fun setAndGetInt_roundTrips() {
        val p = prefs()
        p.setInt("count", 42)
        assertEquals(42, p.getInt("count"))
    }

    @Test
    fun getInt_returnsZero_byDefault() {
        assertEquals(0, prefs().getInt("missing"))
    }

    @Test
    fun rufusQuestType_constant_hasCorrectValue() {
        assertEquals("_rufusQuestType", Preferences.RUFUS_QUEST_TYPE)
    }

    @Test
    fun rufusQuestTarget_constant_hasCorrectValue() {
        assertEquals("_rufusQuestTarget", Preferences.RUFUS_QUEST_TARGET)
    }

    @Test
    fun storedKeys_returnsAllSetKeys() {
        val settings = MapSettings()
        settings.putString("a", "1")
        settings.putBoolean("b", true)
        val p = Preferences(settings)
        assertEquals(setOf("a", "b"), p.storedKeys())
    }

    @Test
    fun removeKey_deletesStoredKey() {
        val p = prefs()
        p.setString("temp", "value")
        assertTrue(p.hasKey("temp"))
        p.removeKey("temp")
        assertFalse(p.hasKey("temp"))
    }
}
