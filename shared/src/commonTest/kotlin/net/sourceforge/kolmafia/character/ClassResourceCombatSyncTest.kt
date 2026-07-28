package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClassResourceCombatSyncTest {

    @Test
    fun parseDiscoMomentum_discomoGif() {
        assertEquals(2, ClassResourceCombatSync.parseDiscoMomentum("""<img src="discomo2.gif">"""))
        assertNull(ClassResourceCombatSync.parseDiscoMomentum("<html>no disco</html>"))
    }

    @Test
    fun apply_updatesCharacterState() {
        val character = KoLCharacter()
        ClassResourceCombatSync.apply(character, """fight discomo3.gif here""")
        assertEquals(3, character.state.value.discoMomentum)
    }
}
