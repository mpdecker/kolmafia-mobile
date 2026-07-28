package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class KitchenAutoBuyTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun willBuyTool_requiresMeatNpcPrefAndNoLimit() {
        val prefs = prefs()
        prefs.setBoolean("autoSatisfyWithNPCs", true)
        assertTrue(
            KitchenAutoBuy.willBuyTool(
                CharacterState(meat = 1000),
                prefs,
            ),
        )
    }

    @Test
    fun willBuyTool_blockedWhenMeatTooLow() {
        val prefs = prefs()
        prefs.setBoolean("autoSatisfyWithNPCs", true)
        assertFalse(
            KitchenAutoBuy.willBuyTool(
                CharacterState(meat = 999),
                prefs,
            ),
        )
    }

    @Test
    fun willBuyTool_blockedInEdLimitMode() {
        val prefs = prefs()
        prefs.setBoolean("autoSatisfyWithNPCs", true)
        assertFalse(
            KitchenAutoBuy.willBuyTool(
                CharacterState(meat = 5000),
                prefs,
                limitMode = "ed",
            ),
        )
    }

    @Test
    fun willBuyServant_requiresMallOrStashPref() {
        val prefs = prefs()
        prefs.setBoolean("autoRepairBoxServants", true)
        prefs.setBoolean("autoSatisfyWithMall", true)
        assertTrue(
            KitchenAutoBuy.willBuyServant(
                prefs,
                CharacterState(),
            ),
        )
    }

    @Test
    fun willBuyServant_blockedInGlover() {
        val prefs = prefs()
        prefs.setBoolean("autoRepairBoxServants", true)
        prefs.setBoolean("autoSatisfyWithMall", true)
        assertFalse(
            KitchenAutoBuy.willBuyServant(
                prefs,
                CharacterState(challengePath = AscensionPath.GLOVER.apiName),
            ),
        )
    }

    @Test
    fun toolCost_is500InBadMoon() {
        assertEquals(
            500,
            KitchenAutoBuy.toolCost(CharacterState(zodiacSign = "Bad Moon")),
        )
    }
}
