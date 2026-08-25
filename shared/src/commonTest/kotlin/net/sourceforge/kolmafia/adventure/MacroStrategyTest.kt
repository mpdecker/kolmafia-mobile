package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.combat.CombatActionManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

class MacroStrategyTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        CombatActionManager.resetForTest()
        ChoiceCombatAshState.reset()
        prefs = Preferences(MapSettings())
    }

    @Test
    fun ccsMacroPreferredOverZonePref() {
        prefs.setString("combatMacroDefault", "zone-fallback-macro")
        CombatActionManager.loadFromText(
            """
            [ default ]
            attack with weapon
            """.trimIndent(),
            preferences = prefs,
        )
        prefs.setString("battleAction", "custom combat script")
        val macro = MacroStrategy.forLocation("15", prefs)
        assertContains(macro, "attack")
        assertEquals(false, macro.contains("zone-fallback-macro"))
    }

    @Test
    fun zonePrefWhenNoCcs() {
        prefs.setString("combatMacro_15", "custom zone macro")
        assertEquals("custom zone macro", MacroStrategy.forLocation("15", prefs))
    }
}
