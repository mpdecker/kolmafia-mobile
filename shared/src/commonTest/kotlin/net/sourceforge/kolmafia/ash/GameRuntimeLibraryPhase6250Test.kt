package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.data.MonsterDrop
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AutoSellRequestHub
import net.sourceforge.kolmafia.request.CrimboHubResponseParse
import net.sourceforge.kolmafia.request.CraftThinHubResponseParse

class GameRuntimeLibraryPhase6250Test {

    @Test
    fun revision_phase6310() {
        assertEquals("phase7150", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun crimboCartel_parsesCrimbux() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CrimboHubResponseParse.parseResponse(
                "crimbo09.php?place=store",
                "You currently have <b>1,234</b> Crimbux.",
                prefs,
            ),
        )
        assertEquals(1234, prefs.getInt("availableCrimbux", 0))
    }

    @Test
    fun crimbo23_delegatesToShopSync() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CrimboHubResponseParse.parseResponse(
                "shop.php?whichshop=crimbo23_elf_bar",
                "You have 42 Elf Guard MPCs.",
                prefs,
            ),
        )
        assertEquals(42, prefs.getInt("availableCrimbo23ElfMpc", 0))
    }

    @Test
    fun craftThinHub_gnomeAndAutosell() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CraftThinHubResponseParse.parseResponse(
                "gnomes.php?action=tinksomething",
                "You acquire an item: <b>thing</b>",
                prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_gnomeTinkerUsed", false))
        assertTrue(
            AutoSellRequestHub.parseResponse(
                "sellstuff.php",
                "You sell some stuff. You gain 500 Meat.",
                prefs,
            ),
        )
        assertEquals(500, prefs.getInt("_lastAutosellMeat", 0))
    }

    @Test
    fun item_drops_array_prefersStatusTracker() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6250 beast",
                id = 62501,
                image = "x.gif",
                attack = 10,
                defense = 10,
                hp = 20,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = listOf(MonsterDrop(itemName = "seal tooth", dropRate = 50.0, prefix = null)),
            ),
            emptyList(),
        )
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            assertEquals(
                "1",
                outputLib(lib, "print(count(item_drops_array()));").trim(),
            )
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }
}
