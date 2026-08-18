package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class GameRuntimeLibraryAshP127Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        NpcStoreDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun npcItemAccessible_honorsValidateProbe() {
        registerItem(9701, "validated npc item")
        NpcStoreDatabase.loadFromText("Probe Shop\topenstore\tvalidated npc item\t50\n")
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals("true", outputLib(lib, """print(npc_item_accessible(9701));""").trim())
    }

    @Test
    fun coinmasterItemAccessible_falseWithoutPref() {
        registerItem(9702, "coin ash item")
        registerItem(9703, "coin ash token")
        CoinmasterDatabase.loadFromText(
            shopsText = "shore\tThe Shore, Inc. Gift Shop\n",
            coinText = "The Shore, Inc. Gift Shop\tROW9702\tcoin ash item (1)\tcoin ash token (5)\n",
        )
        val lib = GameRuntimeLibrary(
            preferences = Preferences(MapSettings()),
            character = net.sourceforge.kolmafia.character.KoLCharacter().also {
                it.updateFromApiResponse(minimalApi(level = "10", king = "1"))
            },
        )
        assertEquals("false", outputLib(lib, """print(coinmaster_item_accessible(9702));""").trim())
    }

    @Test
    fun revision_isphase170() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun minimalApi(level: String = "1", king: String = "0") =
        net.sourceforge.kolmafia.character.CharacterApiResponse(
            name = "t",
            playerid = "1",
            level = level,
            classId = "1",
            sign = "",
            path = "Standard",
            ascensions = "1",
            gender = "m",
            title = "",
            hp = "1",
            hpmax = "1",
            basehpmax = "1",
            mp = "1",
            mpmax = "1",
            basempmax = "1",
            mus = "1",
            musexp = "0",
            mys = "1",
            mysexp = "0",
            mox = "1",
            moxexp = "0",
            buffedmus = "1",
            buffedmys = "1",
            buffedmox = "1",
            meat = "0",
            storagemeat = "0",
            adventures = "0",
            turnsplayed = "0",
            currentrun = "0",
            daycount = "0",
            rollover = "0",
            fullness = "0",
            drunk = "0",
            spleen = "0",
            stomachsize = "15",
            liversize = "14",
            spleensize = "15",
            pvpfights = "0",
            hippystone = "0",
            roninleft = "0",
            hardcore = "0",
            kingliberated = king,
            limitmode = "",
            stills = "-1",
        )
}
