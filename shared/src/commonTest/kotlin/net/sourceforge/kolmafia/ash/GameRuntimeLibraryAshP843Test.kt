package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SaberChoiceSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP843Test {
    @Test
    fun saberForceDropsInvokePostChoiceAutocraft() {
        val prefs = Preferences(MapSettings())
        var crafted = 0
        assertTrue(
            SaberChoiceSync.applyForce(
                choiceId = 1387,
                decision = 3,
                preferences = prefs,
                autoCreateBonerdagonNecklace = { crafted++ },
            ),
        )
        assertEquals(1, crafted)
        assertEquals(1, prefs.getInt("_saberForceUses", 0))
    }
}
