package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AscensionPathTest {

    @Test fun isFistcore_surprisingFistPath_notLiberated() {
        val state = CharacterState(challengePath = "Way of the Surprising Fist", kingLiberated = false)
        assertTrue(state.isFistcore)
    }

    @Test fun isFistcore_surprisingFistPath_kingLiberated_isFalse() {
        val state = CharacterState(challengePath = "Way of the Surprising Fist", kingLiberated = true)
        assertFalse(state.isFistcore)
    }

    @Test fun isFistcore_otherPath_isFalse() {
        val state = CharacterState(challengePath = "Teetotaler")
        assertFalse(state.isFistcore)
    }

    @Test fun isAxecore_avatarOfBoris() {
        val state = CharacterState(challengePath = "Avatar of Boris")
        assertTrue(state.isAxecore)
    }

    @Test fun isAxecore_otherPath_isFalse() {
        val state = CharacterState(challengePath = "None")
        assertFalse(state.isAxecore)
    }
}
