package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.WlfBunkerChoiceSync

class GameRuntimeLibraryAshP886Test {
    @Test
    fun bunkerVisitParsesAndRedeemClearsItems() {
        val prefs = Preferences(MapSettings())
        val html = "<form action=choice.php><b>Job</b><img onclick='descitem(12345)'><span>widgets (3)</span><input name=option value=2></form>"
        assertTrue(WlfBunkerChoiceSync.applyVisit(1093, html, prefs) { if (it == "12345") 777 else null })
        assertFalse(prefs.getBoolean("_volcanoItemRedeemed", true))
        assertEquals(777, prefs.getInt("_volcanoItem2", 0))
        assertEquals(3, prefs.getInt("_volcanoItemCount2", 0))
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(WlfBunkerChoiceSync.apply(1093, 2, "hands you a coin", prefs) { id, qty -> consumed += id to qty })
        assertEquals(listOf(777 to 3), consumed)
        assertTrue(prefs.getBoolean("_volcanoItemRedeemed", false))
    }
}
