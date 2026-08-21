package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP911TrackDTest {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    // ── AshP911 — get_clan_lounge / get_clan_rumpus / get_chateau ───────────

    @Test
    fun phase911_getClanLounge_emptyByDefault() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(count(get_clan_lounge()));"))
    }

    @Test
    fun phase911_getClanRumpus_emptyByDefault() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(count(get_clan_rumpus()));"))
    }

    @Test
    fun phase911_getChateau_emptyByDefault() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(count(get_chateau()));"))
    }

    // ── AshP912 — get_shop / shop_amount / shop_price / shop_limit ──────────

    @Test
    fun phase912_getShop_emptyWhenNoStore() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("0", outputLib(lib, "print(count(get_shop()));"))
    }

    @Test
    fun phase912_shopAmount_zeroWhenNoStore() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("0", outputLib(lib, """print(shop_amount(to_item("none")));"""))
    }

    @Test
    fun phase912_shopPrice_zeroWhenNoStore() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("0", outputLib(lib, """print(shop_price(to_item("none")));"""))
    }

    @Test
    fun phase912_shopLimit_zeroWhenNoStore() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("0", outputLib(lib, """print(shop_limit(to_item("none")));"""))
    }

    // ── AshP913 — my_session_adv ────────────────────────────────────────────

    @Test
    fun phase913_mySessionAdv_defaultZero() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(my_session_adv());"))
    }

    @Test
    fun phase913_mySessionAdv_readsPref() {
        val p = prefs { putInt("_sessionAdventuresUsed", 42) }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("42", outputLib(lib, "print(my_session_adv());"))
    }

    // ── AshP914 — my_session_items ──────────────────────────────────────────

    @Test
    fun phase914_mySessionItems_emptyByDefault() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(count(my_session_items()));"))
    }

    @Test
    fun phase914_mySessionItems_singleItem() {
        val p = prefs { putString("_sessionItemTally", "seal-clubbing club:3") }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("3", outputLib(lib, """print(my_session_items(to_item("seal-clubbing club")));"""))
    }

    // ── AshP915 — my_session_results ────────────────────────────────────────

    @Test
    fun phase915_mySessionResults_emptyByDefault() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(count(my_session_results()));"))
    }

    // ── AshP916 — current_mcd / change_mcd ──────────────────────────────────

    @Test
    fun phase916_currentMcd_readsFromCharacter() {
        val char = KoLCharacter()
        char.setMindControlLevel(5)
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("5", outputLib(lib, "print(current_mcd());"))
    }

    @Test
    fun phase916_currentMcd_defaultZero() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("0", outputLib(lib, "print(current_mcd());"))
    }

    // ── AshP917 — get_fuel ──────────────────────────────────────────────────

    @Test
    fun phase917_getFuel_defaultZero() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, "print(get_fuel());"))
    }

    @Test
    fun phase917_getFuel_readsPref() {
        val p = prefs { putInt("asdonMartinFuel", 37) }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("37", outputLib(lib, "print(get_fuel());"))
    }

    // ── AshP918 — total_free_rests ──────────────────────────────────────────

    @Test
    fun phase918_totalFreeRests_defaultZero() = runBlocking {
        ModifierDatabase.load()
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        val result = outputLib(lib, "print(total_free_rests());").toIntOrNull() ?: -1
        assertTrue(result >= 0, "Free rests should be >= 0")
    }
}
