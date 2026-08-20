package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.PeeVPeeSync
import net.sourceforge.kolmafia.session.PvpManager

class GameRuntimeLibraryAshP481Test {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
    }

    private fun libFromApi(response: CharacterApiResponse): GameRuntimeLibrary {
        val char = KoLCharacter().also { it.updateFromApiResponse(response) }
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun revision_phase482() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pvp_attacks_left_and_hippy_stone_readApi() {
        val lib = libFromApi(CharacterApiResponse(pvpfights = "5", hippystone = "1"))
        assertEquals("5", outputLib(lib, "print(pvp_attacks_left());").trim())
        assertEquals("true", outputLib(lib, "print(hippy_stone_broken());").trim())
    }

    @Test
    fun current_pvp_stances_emptyWhenUnknown() {
        val lib = libFromApi(CharacterApiResponse())
        assertEquals("0", outputLib(lib, "print(count(current_pvp_stances()));").trim())
    }

    @Test
    fun current_pvp_stances_readsCachedMapAfterFightVisit() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(pvpfights = "7", hippystone = "1"))
        }
        val lib = GameRuntimeLibrary(character = char)
        val html = """
            You have 7 fights remaining today.
            <select name="stance"><option value="0" >Bear Hugs All Around</option><option value="1" selected>Beary Famous</option></select>
        """.trimIndent()
        PeeVPeeSync.apply(html, "peevpee.php?place=fight", char)
        assertEquals("2", outputLib(lib, "print(count(current_pvp_stances()));").trim())
        assertEquals("1", outputLib(lib, """print(current_pvp_stances()["Beary Famous"]);""").trim())
        assertEquals("7", outputLib(lib, "print(pvp_attacks_left());").trim())
    }

    @Test
    fun visitHook_fightPage_updatesAttacksAndStances() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(pvpfights = "0", hippystone = "0"))
        }
        val lib = GameRuntimeLibrary(character = char)
        lib.processVisitResponseHooks(
            html = """
                You have 3 fights remaining today.
                <select name="stance"><option value="0" >Bear Hugs All Around</option></select>
            """.trimIndent(),
            url = "https://www.kingdomofloathing.com/peevpee.php?place=fight",
        )
        assertEquals("3", outputLib(lib, "print(pvp_attacks_left());").trim())
        assertEquals("true", outputLib(lib, "print(hippy_stone_broken());").trim())
        assertEquals("1", outputLib(lib, "print(count(current_pvp_stances()));").trim())
    }
}
