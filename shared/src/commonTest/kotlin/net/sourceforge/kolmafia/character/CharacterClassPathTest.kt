package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterClassPathTest {

    @Test
    fun sealClubberPathIsNone() {
        assertEquals(AscensionPath.NONE, CharacterClass.SEAL_CLUBBER.ascensionPath)
    }

    @Test
    fun edPathIsActuallyEd() {
        assertEquals(AscensionPath.ACTUALLY_ED_THE_UNDYING, CharacterClass.ED.ascensionPath)
    }
}
