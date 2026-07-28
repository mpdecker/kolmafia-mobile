package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.KoLCharacter

class GameRuntimeLibraryAshP113Test {

    @Test
    fun mySessionMeat_readsFromCharacterState() {
        val char = KoLCharacter().also { it.addSessionMeat(12_345L) }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("12345", outputLib(lib, """print(my_session_meat());""").trim())
    }

    @Test
    fun visitHook_incrementsSessionMeat() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char)
        lib.processVisitResponseHooks(
            """<p>You gain 1,000 Meat.</p>""",
            "https://www.kingdomofloathing.com/mall.php",
        )
        assertEquals("1000", outputLib(lib, """print(my_session_meat());""").trim())
    }
}
