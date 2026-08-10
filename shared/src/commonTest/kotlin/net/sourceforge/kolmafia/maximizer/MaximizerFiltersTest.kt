package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class MaximizerFiltersTest {

    @Test
    fun parseFromString_readsDesktopPrefFormat() {
        val filters = MaximizerFilters.parseFromString("equip,cast,wish,other,usable,booze,food,spleen")
        assertEquals(MaximizerFilters.allEnabled(), filters)
    }

    @Test
    fun parseFromString_substringMatchIsCaseInsensitive() {
        val filters = MaximizerFilters.parseFromString("CAST,Food")
        assertTrue(MaximizerFilterType.CAST in filters)
        assertTrue(MaximizerFilterType.FOOD in filters)
        assertFalse(MaximizerFilterType.EQUIP in filters)
    }

    @Test
    fun fromPreferences_defaultsToAllWhenBlank() {
        val prefs = Preferences(MapSettings())
        assertEquals(MaximizerFilters.allEnabled(), MaximizerFilters.fromPreferences(prefs))
    }

    @Test
    fun fromPreferences_readsMaximizerLastFilters() {
        val prefs = Preferences(MapSettings()).apply {
            setString("maximizerLastFilters", "cast,food")
        }
        val filters = MaximizerFilters.fromPreferences(prefs)
        assertEquals(setOf(MaximizerFilterType.CAST, MaximizerFilterType.FOOD), filters)
    }

    @Test
    fun allowsSource_mapsDesktopBaseCommands() {
        val all = MaximizerFilters.allEnabled()
        assertTrue(MaximizerFilters.allowsSource("cast 1 Arcane Missile", all))
        assertTrue(MaximizerFilters.allowsSource("eat 1 pizza", all))
        assertTrue(MaximizerFilters.allowsSource("drink 1 beer", all))
        assertTrue(MaximizerFilters.allowsSource("chew 1 gum", all))
        assertTrue(MaximizerFilters.allowsSource("use 1 fortune cookie", all))
        assertTrue(MaximizerFilters.allowsSource("synthesize 1 candy", all))
        assertTrue(MaximizerFilters.allowsSource("genie effect Buff", all))
        assertTrue(MaximizerFilters.allowsSource("monkeypaw effect Buff", all))
        assertTrue(MaximizerFilters.allowsSource("horsery normal horse", all))
    }

    @Test
    fun allowsSource_respectsFilterSubset() {
        val castOnly = setOf(MaximizerFilterType.CAST)
        assertTrue(MaximizerFilters.allowsSource("cast 1 Arcane Missile", castOnly))
        assertFalse(MaximizerFilters.allowsSource("eat 1 pizza", castOnly))
        assertFalse(MaximizerFilters.allowsSource("horsery normal horse", castOnly))
    }

    @Test
    fun isEquipOnly_detectsSingleEquipFilter() {
        assertTrue(MaximizerFilters.isEquipOnly(setOf(MaximizerFilterType.EQUIP)))
        assertFalse(MaximizerFilters.isEquipOnly(setOf(MaximizerFilterType.EQUIP, MaximizerFilterType.CAST)))
    }
}
