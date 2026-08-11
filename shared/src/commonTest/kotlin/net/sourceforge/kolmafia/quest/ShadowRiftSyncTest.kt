package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class ShadowRiftSyncTest {

    @Test
    fun isShadowRiftLocation_matchesShadowRiftPrefix() {
        assertTrue(ShadowRiftSync.isShadowRiftLocation("Shadow Rift (Desert Beach)"))
        assertFalse(ShadowRiftSync.isShadowRiftLocation("Desert Beach"))
    }

    @Test
    fun incrementCombats_incrementsPref() {
        val prefs = Preferences(MapSettings())
        ShadowRiftSync.incrementCombats(prefs)
        ShadowRiftSync.incrementCombats(prefs)
        assertEquals(2, prefs.getInt(ShadowRiftSync.SHADOW_RIFT_COMBATS_PREF, 0))
    }
}
