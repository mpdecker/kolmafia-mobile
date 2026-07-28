package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences

class Crimbo23ShopSyncTest {

    @Test
    fun elfBarSync_parsesElfMpcCount() {
        val prefs = Preferences(MapSettings())
        Crimbo23ShopSync.syncFromShopHtml(
            "You have 42 Elf Guard MPCs to spend.",
            "crimbo23_elf_bar",
            prefs,
        )
        assertEquals(42, prefs.getInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 0))
    }

    @Test
    fun elfBarSync_zeroWhenNoMpcs() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 99)
        Crimbo23ShopSync.syncFromShopHtml(
            "You have no Elf Guard MPCs",
            "crimbo23_elf_bar",
            prefs,
        )
        assertEquals(0, prefs.getInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, -1))
    }

    @Test
    fun pirateCafeSync_parsesPieceOf12Count() {
        val prefs = Preferences(MapSettings())
        Crimbo23ShopSync.syncFromShopHtml(
            "You have 17 Crimbuccaneer pieces of 12.",
            "crimbo23_pirate_cafe",
            prefs,
        )
        assertEquals(17, prefs.getInt(Crimbo23ShopSync.AVAILABLE_PIECE_OF_12_PREF, 0))
    }

    @Test
    fun elfArmorySync_parsesMachineParts() {
        val prefs = Preferences(MapSettings())
        Crimbo23ShopSync.syncFromShopHtml(
            """<td>1,234 piles of Elf Army machine parts</td>""",
            "crimbo23_elf_armory",
            prefs,
        )
        assertEquals(1234, prefs.getInt(Crimbo23ShopSync.AVAILABLE_MACHINE_PARTS_PREF, 0))
    }

    @Test
    fun pirateArmorySync_zeroWhenNoFlotsam() {
        val prefs = Preferences(MapSettings())
        Crimbo23ShopSync.syncFromShopHtml(
            "no piles of Crimbuccaneer flotsam",
            "crimbo23_pirate_armory",
            prefs,
        )
        assertEquals(0, prefs.getInt(Crimbo23ShopSync.AVAILABLE_FLOTSAM_PREF, -1))
    }

    @Test
    fun pirateArmorySync_parsesFlotsam() {
        val prefs = Preferences(MapSettings())
        Crimbo23ShopSync.syncFromShopHtml(
            """<td>88 piles of Crimbuccaneer flotsam</td>""",
            "crimbo23_pirate_armory",
            prefs,
        )
        assertEquals(88, prefs.getInt(Crimbo23ShopSync.AVAILABLE_FLOTSAM_PREF, 0))
    }
}
