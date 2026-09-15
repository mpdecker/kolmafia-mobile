package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.Crimbo23ElfBarRequestHub
import net.sourceforge.kolmafia.request.FunALogRequestHub
import net.sourceforge.kolmafia.request.JunkMagazineRequestHub
import net.sourceforge.kolmafia.request.KOLHSShopRequestHub
import net.sourceforge.kolmafia.session.AvailableCombatSkills
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.FightRamTracker
import net.sourceforge.kolmafia.session.TavernManager

class GameRuntimeLibraryPhase5350Test {

    @Test
    fun revision_phase6310() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
        assertEquals("6850", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun tavernManager_searchOrderFaucetLookup() {
        val layout = "0000000000003000000000000"
        assertEquals(13, TavernManager.faucetSquare(layout))
        assertEquals(0, TavernManager.baronSquare(layout))
        assertEquals(4, TavernManager.nextUnexploredSquare(layout))
    }

    @Test
    fun tavernManager_overrideSquare() {
        TavernManager.overrideSquare = 7
        try {
            assertEquals(8, TavernManager.recommendSquare("0000000000000000000000000", level = 5))
        } finally {
            TavernManager.overrideSquare = -1
        }
    }

    @Test
    fun tavern_ash_returnsKnownFaucetSquare() {
        val prefs = Preferences(MapSettings())
        prefs.setString("tavernLayout", "0000000000003000000000000")
        val lib = GameRuntimeLibrary(preferences = prefs)
        // Level < 3 without exploathing → -1 unless already found (found returns early)
        assertEquals("13", outputLib(lib, "print(tavern());"))
        assertEquals("13", outputLib(lib, """print(tavern("faucet"));"""))
    }

    @Test
    fun my_ram_usesFightRamTracker() {
        ChoiceCombatAshState.reset()
        FightRamTracker.onFightStart(ramModifier = 2)
        ChoiceCombatAshState.currentRound = 2
        assertEquals(5, FightRamTracker.getCurrent(2, 2))
        FightRamTracker.applySkillCost(3)
        assertEquals(2, FightRamTracker.currentRAM)
        val lib = GameRuntimeLibrary()
        assertEquals("2", outputLib(lib, "print(my_ram());"))
        ChoiceCombatAshState.reset()
    }

    @Test
    fun combatSkillAvailable_usesDropdownSet() {
        AvailableCombatSkills.clear()
        AvailableCombatSkills.setFromFightHtml(
            """
            <select name=whichskill>
            <option value="15">Tongue of the Walrus (10 MP)</option>
            </select>
            """.trimIndent(),
        )
        assertTrue(AvailableCombatSkills.has(15))
        AvailableCombatSkills.clear()
    }

    @Test
    fun storage_amount_int_overload_registered() {
        val out = outputLib(GameRuntimeLibrary(), "print(storage_amount(1));")
        assertEquals("0", out)
    }

    @Test
    fun httpHubs_crimbo23AndKolhs() {
        assertTrue(Crimbo23ElfBarRequestHub.registerRequest("shop.php?whichshop=crimbo23_elf_bar"))
        assertFalse(Crimbo23ElfBarRequestHub.registerRequest("adventure.php"))
        assertTrue(FunALogRequestHub.registerRequest("shop.php?whichshop=piraterealm"))
        assertTrue(JunkMagazineRequestHub.registerRequest("shop.php?whichshop=junkmagazine"))
        assertTrue(KOLHSShopRequestHub.registerRequest("shop.php?whichshop=kolhs_shop"))
    }
}
