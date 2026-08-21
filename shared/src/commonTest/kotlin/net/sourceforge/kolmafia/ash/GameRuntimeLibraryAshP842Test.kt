package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SaberChoiceSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP842Test {
    @Test
    fun saberForceBanishAndCopyUpdateState() {
        val prefs = Preferences(MapSettings())
        val banishes = BanishManager(prefs)
        assertTrue(SaberChoiceSync.applyForce(1387, 1, prefs, "fluffy bunny", 100, banishes))
        assertTrue(banishes.isBanished("fluffy bunny", 100))
        assertEquals(1, prefs.getInt("_saberForceUses", 0))

        assertTrue(SaberChoiceSync.applyForce(1387, 2, prefs, "angry bugbear", 101, banishes))
        assertEquals("angry bugbear", prefs.getString("_saberForceMonster", ""))
        assertEquals(3, prefs.getInt("_saberForceMonsterCount", 0))
        assertEquals(2, prefs.getInt("_saberForceUses", 0))
    }
}
