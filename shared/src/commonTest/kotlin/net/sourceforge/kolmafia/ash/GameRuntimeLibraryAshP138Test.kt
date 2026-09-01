package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.FINISHED
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe

class GameRuntimeLibraryAshP138Test {

    @Test
    fun revision_phase173() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun isCoinmasterItem_validateFalseWhenRowExists() = runBlocking {
        CoinmasterDatabase.load()
        val lib = GameRuntimeLibrary()
        assertEquals(
            "true",
            outputLib(lib, """print(is_coinmaster_item(7185));""").trim(),
        )
    }

    @Test
    fun isCoinmasterItem_validateTrueBlockedWithoutBlackMarket() = runBlocking {
        CoinmasterDatabase.load()
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals(
            "false",
            outputLib(lib, """print(is_coinmaster_item(7185, true));""").trim(),
        )
    }

    @Test
    fun coinmasterProbe_respectsBlackMarketQuestGate() {
        val prefs = Preferences(MapSettings())
        prefs.setString(Quest.MACGUFFIN.prefKey, FINISHED)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                7185,
                CharacterState(meat = 999_999),
                prefs,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun coinmasterProbe_allowsZeppelinWhenQuestOpenAndNotOwned() = runBlocking {
        CoinmasterDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Quest.MACGUFFIN.prefKey, FINISHED)
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                7185,
                CharacterState(meat = 999_999),
                prefs,
                accessibleCount = { if (it == 7221) 5 else 0 },
            ),
        )
    }
}
