package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.DripArmoryPrefs

class GameRuntimeLibraryAshP208Test {

    private companion object {
        const val YAK_SKIN = 394
    }

    @AfterTest
    fun tearDown() {
        CoinmasterDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase207() {
        assertEquals("phase230", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun driparmory_validateDeniedUntilVisitHook() {
        registerItem(DripArmoryPrefs.DRIPPY_SHIELD, "drippy shield")
        CoinmasterDatabase.loadFromText(
            shopsText = "driparmory\tDrip Institute Armory\n",
            coinText = "Drip Institute Armory\tbuy\t50\tdrippy shield\tROW1132\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
        )
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item(${DripArmoryPrefs.DRIPPY_SHIELD}, true));""").trim(),
        )
        lib.processVisitResponseHooks(
            html = """<b>drippy shield</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory",
        )
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item(${DripArmoryPrefs.DRIPPY_SHIELD}, true));""").trim(),
        )
    }

    @Test
    fun trapper_validateDeniedUntilVisitHook() {
        registerItem(YAK_SKIN, "yak skin")
        CoinmasterDatabase.loadFromText(
            shopsText = "trapper\tThe Trapper\n",
            coinText = "The Trapper\tbuy\t1\tyak skin\tROW14\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(
                    CharacterApiResponse(
                        meat = "100000",
                        level = "10",
                        ascensions = "4",
                    ),
                )
            },
        )
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item($YAK_SKIN, true));""").trim(),
        )
        lib.processVisitResponseHooks(
            html = """trade your yeti furs for goods""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=trapper",
        )
        assertEquals(4, prefs.getInt("lastTr4pz0rQuest", -1))
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item($YAK_SKIN, true));""").trim(),
        )
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
}
