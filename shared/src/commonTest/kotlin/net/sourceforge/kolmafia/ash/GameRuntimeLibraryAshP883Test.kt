package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.quest.OddJobsBoardChoiceSync

class GameRuntimeLibraryAshP883Test {
    @Test
    fun oddJobsChoiceIsRecognizedWithoutMutation() {
        assertTrue(OddJobsBoardChoiceSync.apply(985))
        assertFalse(OddJobsBoardChoiceSync.apply(984))
    }
}
