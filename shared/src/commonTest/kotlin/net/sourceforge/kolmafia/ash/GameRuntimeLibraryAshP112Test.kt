package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.KoLCharacter

class GameRuntimeLibraryAshP112Test {

    @Test
    fun myClosetMeat_readsFromCharacterState() {
        val char = KoLCharacter().also { it.setClosetMeat(170_000_000L) }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("170000000", outputLib(lib, """print(my_closet_meat());""").trim())
    }

    @Test
    fun myStorageMeat_readsFromCharacterState() {
        val char = KoLCharacter().also { it.setStorageMeat(178_634_761L) }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("178634761", outputLib(lib, """print(my_storage_meat());""").trim())
    }

    @Test
    fun closetHook_updatesClosetMeat() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char)
        lib.processVisitResponseHooks(
            """Your closet contains <b>42,000</b> meat.""",
            "https://www.kingdomofloathing.com/closet.php",
        )
        assertEquals("42000", outputLib(lib, """print(my_closet_meat());""").trim())
    }

    @Test
    fun storageHook_updatesStorageMeat() {
        val char = KoLCharacter()
        val lib = GameRuntimeLibrary(character = char)
        lib.processVisitResponseHooks(
            """<b>You have 99,999 meat in long-term storage.</b>""",
            "https://www.kingdomofloathing.com/storage.php?which=5",
        )
        assertEquals("99999", outputLib(lib, """print(my_storage_meat());""").trim())
    }
}
