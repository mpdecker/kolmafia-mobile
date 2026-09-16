package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.DynamicChoiceSpoilers
import net.sourceforge.kolmafia.session.RequestLogger

class GameRuntimeLibraryPhase5950Test {
    @Test
    fun revisionIsPhase5950() {
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun eggmanAndClancyHelpers() {
        assertEquals(50.0, DynamicChoiceSpoilers.computeEggmanItemDrop(false), 0.001)
        assertEquals(75.0, DynamicChoiceSpoilers.computeEggmanItemDrop(true), 0.001)
        assertTrue(DynamicChoiceSpoilers.computeClancyLuteItemDrop("lute", 7) > 0.0)
        assertEquals(0.0, DynamicChoiceSpoilers.computeEdCatServantItemDrop("Cat", 6), 0.001)
    }

    @Test
    fun autoCreateItemIdsPresent() {
        assertEquals(677, ItemPool.BADASS_BELT)
        assertEquals(1248, ItemPool.BONERDAGON_NECKLACE)
        assertEquals(486, ItemPool.TALISMAN)
        assertEquals(1656, ItemPool.CITADEL_SATCHEL)
    }

    @Test
    fun transferParseFormFields() {
        val prev = RequestLogger.itemNameById
        RequestLogger.itemNameById = { id -> "n$id" }
        try {
            val items = RequestLogger.parseTransferItems(
                "closet.php",
                mapOf("whichitem" to "9", "howmany" to "4"),
            )
            assertEquals(listOf(9 to 4), items)
            val line = RequestLogger.formatTransferLog("send a gift", items)
            assertTrue(line.contains("4 n9"))
        } finally {
            RequestLogger.itemNameById = prev
        }
        val prefs = Preferences(MapSettings())
        assertEquals("", prefs.getString(DynamicChoiceSpoilers.JARLSBERG_COMPANION_PREF, ""))
    }
}
