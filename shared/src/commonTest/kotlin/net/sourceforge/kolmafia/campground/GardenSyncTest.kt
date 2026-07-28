package net.sourceforge.kolmafia.campground

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class GardenSyncTest {

    @Test
    fun parseGardenType_pumpkin() {
        assertEquals(CropType.PUMPKIN, GardenSync.parseGardenType("""<img src="pumpkinpatch_2.gif">"""))
    }

    @Test
    fun parseGardenType_mushroom() {
        assertEquals(CropType.MUSHROOM, GardenSync.parseGardenType("""<img src="mushgarden.gif">"""))
    }

    @Test
    fun parseGardenType_rock() {
        assertEquals(CropType.ROCK, GardenSync.parseGardenType("""<img src="/rockgarden/a1.gif">"""))
    }

    @Test
    fun parseGardenType_emptyReturnsNull() {
        assertNull(GardenSync.parseGardenType("<html><body>No garden</body></html>"))
    }

    @Test
    fun apply_updatesCharacterStateAndPref() {
        val prefs = Preferences(MapSettings())
        val character = KoLCharacter()
        GardenSync.apply(character, """<img src="beergarden3.gif">""", prefs)
        assertEquals("beer", character.state.value.gardenType)
        assertEquals("beer", prefs.getString("myGardenType", ""))
    }

    @Test
    fun apply_clearsWhenNoGarden() {
        val prefs = Preferences(MapSettings())
        prefs.setString("myGardenType", "pumpkin")
        val character = KoLCharacter().also { it.setCampground(gardenType = "pumpkin") }
        GardenSync.apply(character, "<html></html>", prefs)
        assertEquals("", character.state.value.gardenType)
        assertEquals("", prefs.getString("myGardenType", "stale"))
    }
}
