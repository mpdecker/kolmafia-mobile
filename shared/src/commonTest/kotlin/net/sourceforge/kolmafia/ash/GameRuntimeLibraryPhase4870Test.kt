package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CombineMeatRequest
import net.sourceforge.kolmafia.request.Crimbo09Request
import net.sourceforge.kolmafia.request.Crimbo10Request

class GameRuntimeLibraryPhase4870Test {

    @Test
    fun revision_was_phase4870_batch() {
        // Historical XVI coverage; live revision advances with later megas.
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
        assertEquals(
            GameRuntimeLibrary.REVISION,
            outputLib(GameRuntimeLibrary(), "print(get_revision());"),
        )
    }

    @Test
    fun sell_returnsBoolean() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """boolean b = sell(to_coinmaster("none"), 1, to_item("none")); print(b);""",
        )
        assertTrue(out == "true" || out == "false")
    }

    @Test
    fun put_display_itemFirst_overloadExists() {
        // Missing displayCaseRequest → false, but overload must compile
        assertEquals(
            "false",
            outputLib(
                GameRuntimeLibrary(),
                """print(put_display(to_item("seal tooth"), 2));""",
            ),
        )
    }

    @Test
    fun take_stash_itemFirst_overloadExists() {
        assertEquals(
            "false",
            outputLib(
                GameRuntimeLibrary(),
                """print(take_stash(to_item("seal tooth"), 1));""",
            ),
        )
    }

    @Test
    fun craft_zeroCount_returnsZero() {
        assertEquals(
            "0",
            outputLib(
                GameRuntimeLibrary(),
                """print(craft("combine", 0, to_item("none"), to_item("none")));""",
            ),
        )
    }

    @Test
    fun collectionCache_adjust() {
        val prefs = Preferences(MapSettings())
        CollectionCache.save(prefs, Preferences.CACHED_DISPLAY, mapOf(1 to 5))
        CollectionCache.adjust(prefs, Preferences.CACHED_DISPLAY, 1, 2)
        assertEquals(7, CollectionCache.load(prefs, Preferences.CACHED_DISPLAY)[1])
        CollectionCache.adjust(prefs, Preferences.CACHED_DISPLAY, 1, -10)
        assertTrue(CollectionCache.load(prefs, Preferences.CACHED_DISPLAY).isEmpty())
    }

    @Test
    fun crimbo09_10_and_combineMeat_register() {
        assertTrue(Crimbo09Request.registerRequest("crimbo09.php?place=don"))
        assertTrue(Crimbo09Request.registerRequest("crimbo09.php?action=tradearmbands"))
        assertFalse(Crimbo09Request.registerRequest("choice.php?whichchoice=1"))
        assertTrue(Crimbo10Request.registerRequest("crimbo10.php?place=giftshop"))
        assertEquals("the Gift Shop", Crimbo10Request.locationName("crimbo10.php?place=giftshop"))
        assertEquals(10, CombineMeatRequest.getCost(25))
        assertTrue(CombineMeatRequest.registerRequest("craft.php?action=makepaste&whichitem=25&qty=3"))
        assertFalse(CombineMeatRequest.registerRequest("craft.php?action=combine&whichitem=1"))
    }
}
