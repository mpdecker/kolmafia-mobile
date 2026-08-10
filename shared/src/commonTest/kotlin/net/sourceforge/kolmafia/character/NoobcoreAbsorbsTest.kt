package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals

class NoobcoreAbsorbsTest {

    @Test
    fun absorbsLimit_capsAtFifteenAfterLevelTwelve() {
        assertEquals(3, NoobcoreAbsorbs.absorbsLimit(1))
        assertEquals(14, NoobcoreAbsorbs.absorbsLimit(12))
        assertEquals(15, NoobcoreAbsorbs.absorbsLimit(13))
        assertEquals(15, NoobcoreAbsorbs.absorbsLimit(30))
    }

    @Test
    fun absorbsRemaining_neverNegative() {
        val state = CharacterState(level = 10, absorbs = 8)
        assertEquals(4, NoobcoreAbsorbs.absorbsRemaining(state))
        assertEquals(0, NoobcoreAbsorbs.absorbsRemaining(state.copy(absorbs = 20)))
    }
}
