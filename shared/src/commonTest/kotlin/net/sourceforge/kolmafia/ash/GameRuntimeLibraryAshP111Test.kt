package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP111Test {

    @Test
    fun myGardenType_readsFromCharacterState() {
        val char = KoLCharacter().also { it.setCampground(gardenType = "mushroom") }
        val prefs = Preferences(MapSettings())
        prefs.setString("myGardenType", "pumpkin")
        val lib = GameRuntimeLibrary(character = char, preferences = prefs)
        assertEquals("mushroom", outputLib(lib, """print(my_garden_type());""").trim())
    }

    @Test
    fun myGardenType_prefFallbackWhenStateEmpty() {
        val prefs = Preferences(MapSettings())
        prefs.setString("myGardenType", "winter")
        val lib = GameRuntimeLibrary(character = KoLCharacter(), preferences = prefs)
        assertEquals("winter", outputLib(lib, """print(my_garden_type());""").trim())
    }

    @Test
    fun myGardenType_defaultsToNone() {
        val lib = GameRuntimeLibrary(character = KoLCharacter(), preferences = Preferences(MapSettings()))
        assertEquals("none", outputLib(lib, """print(my_garden_type());""").trim())
    }

    @Test
    fun campgroundHook_updatesGardenType() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char)
        lib.processVisitResponseHooks(
            """<img src="grassgarden4.gif">""",
            "https://www.kingdomofloathing.com/campground.php",
        )
        assertEquals("grass", outputLib(lib, """print(my_garden_type());""").trim())
    }

    @Test
    fun revision_phase156() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }
}
