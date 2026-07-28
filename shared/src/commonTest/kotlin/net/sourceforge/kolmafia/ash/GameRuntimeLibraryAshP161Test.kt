package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterShopSync
import net.sourceforge.kolmafia.shop.SeptEmberSync
import net.sourceforge.kolmafia.shop.SpinMasterLatheSync
import net.sourceforge.kolmafia.shop.TrapperSync

class GameRuntimeLibraryAshP161Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase184() {
        assertEquals("phase190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shopVisitHook_appliesTrapperSync() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(
            preferences = p,
            character = KoLCharacter().apply {
                updateFromApiResponse(
                    net.sourceforge.kolmafia.character.CharacterApiResponse(ascensions = "5"),
                )
            },
        )
        lib.processVisitResponseHooks(
            html = """I'm plumb stocked up on everythin' 'cept yeti furs, Adventurer.""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=trapper",
        )
        assertEquals(5, p.getInt("lastTr4pz0rQuest", -1))
        assertEquals(QuestDatabase.FINISHED, p.getString(Quest.TRAPPER.prefKey, QuestDatabase.UNSTARTED))
    }

    @Test
    fun shopVisitHook_appliesSeptemberSync() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        lib.processVisitResponseHooks(
            html = """<b>You have 42 Embers.</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=september",
        )
        assertTrue(p.getBoolean(SeptEmberSync.BALANCE_CHECKED_PREF, false))
        assertEquals(42, p.getInt(SeptEmberSync.AVAILABLE_EMBERS_PREF, 0))
    }
}

class GameRuntimeLibraryAshP161SyncUnitTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun trapper_syncSetsQuestPrefsWhenYetiFursPresent() {
        val p = prefs()
        TrapperSync.syncFromShopHtml(
            html = "trade your yeti furs for goods",
            prefs = p,
            ascensionNumber = 7,
        )
        assertEquals(7, p.getInt("lastTr4pz0rQuest", -1))
        assertEquals(QuestDatabase.FINISHED, p.getString(Quest.TRAPPER.prefKey, QuestDatabase.UNSTARTED))
    }

    @Test
    fun lathe_syncSetsVisitedPref() {
        val p = prefs()
        SpinMasterLatheSync.syncFromShopHtml(p)
        assertTrue(p.getBoolean(SpinMasterLatheSync.VISITED_PREF, false))
    }

    @Test
    fun trapper_skipsBuyActionUrl() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = "yeti furs",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=trapper&action=buy",
            prefs = p,
            state = CharacterState(ascensionNumber = 3),
        )
        assertEquals(-1, p.getInt("lastTr4pz0rQuest", -1))
    }
}
