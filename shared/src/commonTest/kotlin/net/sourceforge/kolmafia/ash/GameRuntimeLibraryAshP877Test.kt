package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FloristFriarChoiceSync

class GameRuntimeLibraryAshP877Test {
    @Test
    fun floristAddsAndDigsPlants() {
        val prefs = Preferences(MapSettings())
        FloristFriarChoiceSync.reset()
        assertTrue(FloristFriarChoiceSync.apply(720, "choice.php?whichchoice=720&option=1&plant=7", "The Florist Friar's Cottage Ah, <b>The Sleazy Back Alley</b>!", prefs))
        assertEquals(listOf(7), FloristFriarChoiceSync.plantsAt("The Sleazy Back Alley"))
        assertTrue(FloristFriarChoiceSync.apply(720, "choice.php?whichchoice=720&option=2&plnti=0", "You dig up a plant. Ah, <b>The Sleazy Back Alley</b>!", prefs))
        assertEquals(emptyList(), FloristFriarChoiceSync.plantsAt("The Sleazy Back Alley"))
    }
}
