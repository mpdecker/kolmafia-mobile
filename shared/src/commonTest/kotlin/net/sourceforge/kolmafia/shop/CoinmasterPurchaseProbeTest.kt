package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class CoinmasterPurchaseProbeTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun validate_falseWhenPrefDisabled() {
        registerItem(9501, "shore prize")
        registerItem(9502, "shore token")
        CoinmasterDatabase.loadFromText(
            shopsText = "shore\tThe Shore, Inc. Gift Shop\n",
            coinText = "The Shore, Inc. Gift Shop\tROW9501\tshore prize (1)\tshore token (10)\n",
        )
        val prefs = Preferences(MapSettings())
        val state = CharacterState(level = 10, kingLiberated = true)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                9501,
                state,
                prefs,
            ) { if (it == 9502) 5 else 0 },
        )
    }

    @Test
    fun validate_trueWhenAccessibleAndAffordable() {
        registerItem(9503, "shore prize two")
        registerItem(9504, "shore token two")
        CoinmasterDatabase.loadFromText(
            shopsText = "shore\tThe Shore, Inc. Gift Shop\n",
            coinText = "The Shore, Inc. Gift Shop\tROW9503\tshore prize two (1)\tshore token two (10)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val state = CharacterState(level = 10, kingLiberated = true)
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                9503,
                state,
                prefs,
            ) { if (it == 9504) 20 else 0 },
        )
        assertTrue(
            CoinmasterDatabase.containsBuyItem(
                9503,
                validate = true,
                state = state,
                prefs = prefs,
            ) { if (it == 9504) 20 else 0 },
        )
    }

    @Test
    fun validate_falseWhenMasterInaccessible() {
        registerItem(9505, "dime prize")
        registerItem(9506, "dime token")
        CoinmasterDatabase.loadFromText(
            shopsText = "dmt\tDimemaster\n",
            coinText = "Dimemaster\tROW9505\tdime prize (1)\tdime token (1)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val state = CharacterState(kingLiberated = false)
        assertFalse(
            CoinmasterDatabase.containsBuyItem(
                9505,
                validate = true,
                state = state,
                prefs = prefs,
            ) { 99 },
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
