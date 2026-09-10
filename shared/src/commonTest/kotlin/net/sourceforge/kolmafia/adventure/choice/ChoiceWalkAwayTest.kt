package net.sourceforge.kolmafia.adventure.choice

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChoiceWalkAwayTest {

    @Test
    fun allowList_includesKnownWalkableChoices() {
        assertTrue(ChoiceWalkAway.canWalkFromChoice(1076)) // Mayo Minder
        assertTrue(ChoiceWalkAway.canWalkFromChoice(1181)) // Witchess
        assertTrue(ChoiceWalkAway.canWalkFromChoice(1217)) // Sweet Synthesis
        assertTrue(ChoiceWalkAway.canWalkFromChoice(1510)) // Burning Leaves
        assertTrue(ChoiceWalkAway.canWalkFromChoice(1596)) // Dig at Zone
        assertTrue(ChoiceWalkAway.canWalkFromChoice(1543)) // Parachute into a Fight
        assertTrue(ChoiceWalkAway.canWalkFromChoice(1601)) // Cup of 13s
    }

    @Test
    fun default_deniesUnknownChoices() {
        assertFalse(ChoiceWalkAway.canWalkFromChoice(1))
        assertFalse(ChoiceWalkAway.canWalkFromChoice(9999))
        assertFalse(ChoiceWalkAway.canWalkFromChoice(0))
    }
}
