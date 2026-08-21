package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SkeletonClosetChoiceSync

class GameRuntimeLibraryAshP704Test {

    @Test
    fun revision_phase707() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun skeleton_consumesUnlessDecision6() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            SkeletonClosetChoiceSync.apply(603, 1) { id, qty -> consumed.add(id to qty) },
        )
        assertTrue(consumed.contains(SkeletonClosetChoiceSync.SKELETON to 1))
        consumed.clear()
        assertFalse(
            SkeletonClosetChoiceSync.apply(603, 6) { id, qty -> consumed.add(id to qty) },
        )
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun questChoiceRules_wires603() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 603,
                responseText = "",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
    }
}
