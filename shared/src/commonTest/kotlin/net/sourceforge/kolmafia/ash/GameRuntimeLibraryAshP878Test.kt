package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.quest.TalesOfDreadChoiceSync

class GameRuntimeLibraryAshP878Test {
    @Test
    fun talesChoiceIsRecognizedWithoutMutation() {
        assertTrue(TalesOfDreadChoiceSync.apply(767))
        assertFalse(TalesOfDreadChoiceSync.apply(768))
    }
}
