package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckOtherRedirectionTest {

    @Test
    fun godLobster_incrementsAndLabels() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_godLobsterFights", 1)
        val result = CheckOtherRedirection.apply(
            location = "main.php?fightgodlobster=1",
            preferences = prefs,
        )
        assertTrue(result.handled)
        assertEquals("God Lobster", result.label)
        assertEquals(2, prefs.getInt("_godLobsterFights", 0))
        assertEquals("God Lobster", CheckItemRedirection.itemMonster)
        CheckItemRedirection.clearItemMonster()
    }

    @Test
    fun boneGarden_labels() {
        val result = CheckOtherRedirection.apply(
            location = "campground.php?action=garden",
            preferences = Preferences(MapSettings()),
        )
        assertTrue(result.handled)
        assertEquals("Bone Garden", result.label)
        CheckItemRedirection.clearItemMonster()
    }

    @Test
    fun unrelated_notHandled() {
        val result = CheckOtherRedirection.apply(
            location = "adventure.php?snarfblat=1",
            preferences = null,
        )
        assertFalse(result.handled)
    }
}
