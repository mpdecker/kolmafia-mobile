package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase

class GameRuntimeLibraryAshP115Test {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun pullsRemaining_defaultsToNegativeOne() {
        ConcoctionDatabase.resetForTest()
        val lib = GameRuntimeLibrary(character = KoLCharacter())
        assertEquals("-1", outputLib(lib, """print(pulls_remaining());""").trim())
    }

    @Test
    fun pullsRemaining_readsFromConcoctionDatabase() {
        ConcoctionDatabase.setPullsRemaining(42)
        val lib = GameRuntimeLibrary(character = KoLCharacter())
        assertEquals("42", outputLib(lib, """print(pulls_remaining());""").trim())
    }

    @Test
    fun storageHook_updatesPullsRemaining() {
        ConcoctionDatabase.resetForTest()
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char)
        lib.processVisitResponseHooks(
            """<b>You have 99,999 meat in long-term storage.</b> <span class="pullsleft">3</span>""",
            "https://www.kingdomofloathing.com/storage.php?which=5",
        )
        assertEquals("3", outputLib(lib, """print(pulls_remaining());""").trim())
    }

    @Test
    fun revision_phase160() {
        assertEquals("phase450", GameRuntimeLibrary.REVISION)
    }
}
