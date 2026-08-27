package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP429Test {

    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun workshedDb(): GameDatabase = object : GameDatabase() {
        private val chem = ItemData(
            6967, "Chemystery Box", "desc", "wbchemset.gif",
            ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null,
        )
        override fun item(id: Int): ItemData? = if (id == 6967) chem else null
        override fun item(name: String): ItemData? =
            if (name.equals("Chemystery Box", ignoreCase = true)) chem else null
    }

    @Test
    fun revision_phase480() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun get_workshed_readsSeededPref() {
        val p = prefs {
            setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, 6967)
        }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = workshedDb())
        assertEquals("Chemystery Box", outputLib(lib, """print(get_workshed());""").trim())
    }

    @Test
    fun get_workshed_emptyWhenUnset() {
        val lib = GameRuntimeLibrary(preferences = prefs(), gameDatabase = workshedDb())
        assertEquals("", outputLib(lib, """print(get_workshed());""").trim())
    }

    @Test
    fun have_campground_falseForRobocore() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = AscensionPath.YOU_ROBOT.apiName))
        }
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("false", outputLib(lib, """print(have_campground());""").trim())
    }

    @Test
    fun have_campground_trueForStandard() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = AscensionPath.STANDARD.apiName))
        }
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        assertEquals("true", outputLib(lib, """print(have_campground());""").trim())
    }

    @Test
    fun have_chef_and_bartender_readPrefs() {
        val p = prefs {
            setBoolean("hasChef", true)
            setBoolean("hasBartender", true)
        }
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("true", outputLib(lib, """print(have_chef());""").trim())
        assertEquals("true", outputLib(lib, """print(have_bartender());""").trim())
    }
}
