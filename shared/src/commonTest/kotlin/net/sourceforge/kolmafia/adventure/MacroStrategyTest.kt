package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class MacroStrategyTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun fallsBackToSafeDefault_whenNothingSet() {
        assertEquals(MacroStrategy.SAFE_DEFAULT, MacroStrategy.forLocation("1", prefs()))
    }

    @Test
    fun usesGlobalDefault_whenSet() {
        val p = prefs()
        p.setString("combatMacroDefault", "skill 3004")
        assertEquals("skill 3004", MacroStrategy.forLocation("1", p))
    }

    @Test
    fun usesPerZoneOverride_whenSet() {
        val p = prefs()
        p.setString("combatMacroDefault", "skill 3004")
        p.setString("combatMacro_1", "skill 3005")
        assertEquals("skill 3005", MacroStrategy.forLocation("1", p))
    }

}
