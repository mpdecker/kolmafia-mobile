package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class BangPotionResolverTest {

    @Test
    fun resolveItemId_bangPotion_matchesPrefSuffix() {
        val prefs = Preferences(MapSettings()).apply {
            setString("lastBangPotion821", "explosiveness")
        }
        assertEquals(
            821,
            BangPotionResolver.resolveItemId("potion of explosiveness", prefs),
        )
    }

    @Test
    fun resolveItemId_bangPotion_noPrefMatch_returnsNull() {
        val prefs = Preferences(MapSettings())
        assertNull(BangPotionResolver.resolveItemId("potion of explosiveness", prefs))
    }

    @Test
    fun resolveItemId_bangPotion_emptyPref_returnsNull() {
        val prefs = Preferences(MapSettings()).apply {
            setString("lastBangPotion819", "")
        }
        assertNull(BangPotionResolver.resolveItemId("potion of bang", prefs))
    }

    @Test
    fun resolveItemId_slimeVial_matchesPrefSuffix() {
        val prefs = Preferences(MapSettings()).apply {
            setString("lastSlimeVial3886", "slimy strength")
        }
        assertEquals(
            3886,
            BangPotionResolver.resolveItemId("vial of slime: slimy strength", prefs),
        )
    }

    @Test
    fun resolveItemId_slimeVial_scansUpToLastExclusive() {
        val prefs = Preferences(MapSettings()).apply {
            setString("lastSlimeVial${ItemDatabase.LAST_SLIME_VIAL - 1}", "final vial")
        }
        assertEquals(
            ItemDatabase.LAST_SLIME_VIAL - 1,
            BangPotionResolver.resolveItemId("vial of slime: final vial", prefs),
        )
        assertNull(
            BangPotionResolver.resolveItemId(
                "vial of slime: final vial",
                Preferences(MapSettings()).apply {
                    setString("lastSlimeVial${ItemDatabase.LAST_SLIME_VIAL}", "out of range")
                },
            ),
        )
    }

    @Test
    fun resolveItemId_nonAliasName_returnsNull() {
        val prefs = Preferences(MapSettings()).apply {
            setString("lastBangPotion819", "explosiveness")
        }
        assertNull(BangPotionResolver.resolveItemId("generic healing potion", prefs))
    }

    @Test
    fun resolveItemId_nullPreferences_returnsNull() {
        assertNull(BangPotionResolver.resolveItemId("potion of explosiveness", null))
    }
}
