package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.data.PokefamDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class PokefamBoostSyncTest {

    @Test
    fun getPokeBoost_readsRaceFromPref() {
        val prefs = Preferences(MapSettings())
        prefs.setString(PokefamBoostSync.POKEFAM_BOOSTS_PREF, "Globmule:Power|Bluzzard:HP")
        assertEquals(PokeBoost.POWER, PokefamBoostSync.getPokeBoost("Globmule", prefs))
        assertEquals(PokeBoost.HP, PokefamBoostSync.getPokeBoost("Bluzzard", prefs))
        assertEquals(PokeBoost.NONE, PokefamBoostSync.getPokeBoost("Missing", prefs))
    }

    @Test
    fun syncFromFeed_appendsBoostEntry() = runTest {
        FamiliarDefinitionDatabase.load()
        val prefs = Preferences(MapSettings())
        val url = "famteam.php?action=feed&fam=215&iid=9748"
        val html = "<html><center>Familiar powered up.</center></html>"
        PokefamBoostSync.syncFromFeed(url, html, prefs)
        assertEquals("Globmule:Power", prefs.getString(PokefamBoostSync.POKEFAM_BOOSTS_PREF))
    }

    @Test
    fun syncFromFeed_usesNoneWhenBoostMatchesNaturalAttribute() = runTest {
        FamiliarDefinitionDatabase.load()
        PokefamDatabase.load()
        val prefs = Preferences(MapSettings())
        val url = "famteam.php?action=feed&fam=8&iid=9750"
        val html = "<html><center>Familiar powered up.</center></html>"
        PokefamBoostSync.syncFromFeed(url, html, prefs)
        assertEquals("Barrrnacle:None", prefs.getString(PokefamBoostSync.POKEFAM_BOOSTS_PREF))
    }

    @Test
    fun adjustStats_subtractsPowerHpAndRemovesAttributeBoosts() {
        val prefs = Preferences(MapSettings())
        prefs.setString(PokefamBoostSync.POKEFAM_BOOSTS_PREF, "Globmule:Power")
        val (power, hp, attrs) = PokefamBoostSync.adjustStats(
            race = "Globmule",
            power = 3,
            hp = 4,
            attributes = listOf("Smart"),
            preferences = prefs,
        )
        assertEquals(2, power)
        assertEquals(4, hp)
        assertEquals(listOf("Smart"), attrs)

        prefs.setString(PokefamBoostSync.POKEFAM_BOOSTS_PREF, "Globmule:HP")
        val hpAdjusted = PokefamBoostSync.adjustStats("Globmule", 3, 4, listOf("Smart"), prefs)
        assertEquals(3, hpAdjusted.first)
        assertEquals(3, hpAdjusted.second)

        prefs.setString(PokefamBoostSync.POKEFAM_BOOSTS_PREF, "Globmule:Armor")
        val armorAdjusted = PokefamBoostSync.adjustStats("Globmule", 3, 4, listOf("Armor", "Smart"), prefs)
        assertEquals(listOf("Smart"), armorAdjusted.third)
    }
}
