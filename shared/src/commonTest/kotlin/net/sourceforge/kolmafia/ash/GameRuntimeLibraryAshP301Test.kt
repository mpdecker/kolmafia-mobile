package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.clan.ClanManager

class GameRuntimeLibraryAshP301Test {

    @AfterTest
    fun tearDown() {
        ClanManager.resetForTest()
    }

    @Test
    fun revision_isphase303() {
        assertEquals("phase490", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun get_clan_id_returnsClanManagerValue() {
        ClanManager.setClan(4242, "Test Clan")
        val lib = GameRuntimeLibrary()
        assertEquals("4242", outputLib(lib, """print(get_clan_id());""").trim())
    }

    @Test
    fun get_clan_name_returnsClanManagerValue() {
        ClanManager.setClan(4242, "Test Clan")
        val lib = GameRuntimeLibrary()
        assertEquals("Test Clan", outputLib(lib, """print(get_clan_name());""").trim())
    }

    @Test
    fun get_clan_id_withoutClan_returnsZero() {
        val lib = GameRuntimeLibrary()
        assertEquals("0", outputLib(lib, """print(get_clan_id());""").trim())
    }
}
