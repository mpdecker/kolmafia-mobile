package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.DefaultsDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.TurnCounter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP905TrackCTest {

    @BeforeTest
    fun setUp() = runBlocking {
        DefaultsDatabase.load()
    }

    @AfterTest
    fun tearDown() {
    }

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    // ── AshP905 — property_exists ───────────────────────────────────────────

    @Test
    fun phase905_propertyExists_returnsTrueForExistingPref() {
        val p = prefs { putString("testProp", "hello") }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("true", outputLib(lib, """print(property_exists("testProp"));"""))
    }

    @Test
    fun phase905_propertyExists_returnsFalseForMissingPref() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("false", outputLib(lib, """print(property_exists("nonExistentProp12345"));"""))
    }

    @Test
    fun phase905_propertyExists_choiceAdventureBuiltIn() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("true", outputLib(lib, """print(property_exists("choiceAdventure100"));"""))
    }

    @Test
    fun phase905_propertyExists_twoArgOverload() {
        val p = prefs { putString("myKey", "val") }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("true", outputLib(lib, """print(property_exists("myKey", false));"""))
    }

    // ── AshP906 — property_has_default / property_default_value ─────────────

    @Test
    fun phase906_propertyHasDefault_knownDefaultReturnsTrue() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val known = DefaultsDatabase.has("autoRecoverHp")
        if (known) {
            assertEquals("true", outputLib(lib, """print(property_has_default("autoRecoverHp"));"""))
        }
    }

    @Test
    fun phase906_propertyDefaultValue_returnsDefault() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val known = DefaultsDatabase.has("autoRecoverHp")
        if (known) {
            val defaultVal = DefaultsDatabase.getString("autoRecoverHp")
            assertEquals(defaultVal, outputLib(lib, """print(property_default_value("autoRecoverHp"));"""))
        }
    }

    @Test
    fun phase906_propertyHasDefault_unknownReturnsFalse() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("false", outputLib(lib, """print(property_has_default("zzzNoSuchDefault999"));"""))
    }

    // ── AshP907 — get_all_properties ────────────────────────────────────────

    @Test
    fun phase907_getAllProperties_filtersCorrectly() {
        val p = prefs {
            putString("testFilter_aaa", "1")
            putString("testFilter_bbb", "2")
            putString("other", "3")
        }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("2", outputLib(lib, """print(count(get_all_properties("testFilter", false)));"""))
    }

    // ── AshP908 — remove_property / rename_property ─────────────────────────

    @Test
    fun phase908_removeProperty_returnsOldValueAndRemoves() {
        val p = prefs { putString("removable", "oldval") }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("oldval", outputLib(lib, """print(remove_property("removable"));"""))
        assertEquals("false", outputLib(lib, """print(property_exists("removable"));"""))
    }

    @Test
    fun phase908_renameProperty_movesValue() {
        val p = prefs { putString("oldKey", "myValue") }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("true", outputLib(lib, """print(rename_property("oldKey", "newKey"));"""))
        assertEquals("false", outputLib(lib, """print(property_exists("oldKey"));"""))
        assertEquals("myValue", outputLib(lib, """print(get_property("newKey"));"""))
    }

    @Test
    fun phase908_renameProperty_failsForBuiltIn() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val known = DefaultsDatabase.has("autoRecoverHp")
        if (known) {
            assertEquals("false", outputLib(lib, """print(rename_property("autoRecoverHp", "newName"));"""))
        }
    }

    // ── AshP909 — get_counter / get_counters ────────────────────────────────

    @Test
    fun phase909_getCounter_returnsTurnsRemaining() {
        val p = prefs()
        TurnCounter.startCounting(p, 100, 10, "Test Counter", "clock.gif")
        val char = KoLCharacter()
        char.setCurrentRun(102)
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        assertEquals("8", outputLib(lib, """print(get_counter("Test Counter"));"""))
    }

    @Test
    fun phase909_getCounter_returnsNegOneForMissing() {
        val p = prefs()
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        assertEquals("-1", outputLib(lib, """print(get_counter("No Such Counter"));"""))
    }

    @Test
    fun phase909_getCounters_returnsLabelsInRange() {
        val p = prefs()
        TurnCounter.startCounting(p, 100, 5, "Alpha", "a.gif")
        TurnCounter.startCounting(p, 100, 20, "Beta", "b.gif")
        val char = KoLCharacter()
        char.setCurrentRun(100)
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        val result = outputLib(lib, """print(get_counters("", 0, 10));""")
        assertTrue(result.contains("Alpha"), "Should contain Alpha in range")
    }

    // ── AshP910 — stop_counter ──────────────────────────────────────────────

    @Test
    fun phase910_stopCounter_removesCounter() {
        val p = prefs()
        TurnCounter.startCounting(p, 100, 10, "Removable", "x.gif")
        val char = KoLCharacter()
        char.setCurrentRun(100)
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        assertEquals("10", outputLib(lib, """print(get_counter("Removable"));"""))
        outputLib(lib, """stop_counter("Removable");""")
        assertEquals("-1", outputLib(lib, """print(get_counter("Removable"));"""))
    }
}
