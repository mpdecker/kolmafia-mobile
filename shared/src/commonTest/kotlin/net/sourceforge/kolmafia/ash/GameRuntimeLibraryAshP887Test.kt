package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SpacegateLeftoversChoiceSync

class GameRuntimeLibraryAshP887Test {
    @Test
    fun leftoversLogLovConsumeMemoryAndResetAvatar() {
        val prefs = Preferences(MapSettings())
        val logs = mutableListOf<String>()
        assertTrue(SpacegateLeftoversChoiceSync.applyVisit(1229, "a sign above it that says <b>TRUST</b>", logs::add))
        assertTrue(SpacegateLeftoversChoiceSync.apply(1229, 1, "you scrawl <b>HOPE</b>", prefs, sessionLog = logs::add))
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(SpacegateLeftoversChoiceSync.apply(1231, 1, "", prefs, { id, qty -> consumed += id to qty }))
        var resetClass = ""
        assertTrue(SpacegateLeftoversChoiceSync.apply(1232, 4, "", prefs, resetAfterAvatar = { resetClass = it }))
        assertEquals(listOf("L.O.V. Exit word: TRUST", "Your log entry: HOPE"), logs)
        assertEquals(listOf(9345 to 1), consumed)
        assertEquals(5, prefs.getInt("noobDeferredPoints", 0))
        assertEquals("Sauceror", resetClass)
    }
}
