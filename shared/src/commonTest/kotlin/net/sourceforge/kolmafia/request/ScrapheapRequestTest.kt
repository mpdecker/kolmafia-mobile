package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScrapheapRequestTest {

    @Test
    fun chronolith_deductsEnergyAndSetsNextCost() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_chronolithNextCost", 15)
        val character = KoLCharacter()
        character.setYouRobotEnergy(100)
        ScrapheapRequest.parseResponse(
            "place.php?whichplace=scrapheap&action=sh_chronolith",
            """You gain 10 Adventures. <a title="(25 Energy)">Chronolith</a>""",
            prefs,
            character,
        )
        assertEquals(85, character.state.value.youRobotEnergy)
        assertEquals(25, prefs.getInt("_chronolithNextCost", 0))
    }

    @Test
    fun scrounge_setsScavengedFlag() {
        val prefs = Preferences(MapSettings())
        ScrapheapRequest.parseResponse(
            "place.php?whichplace=scrapheap&action=sh_scrounge",
            "You find some scrap.",
            prefs,
            null,
        )
        assertTrue(prefs.getBoolean("youRobotScavenged", false))
    }
}
