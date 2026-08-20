package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.UNSTARTED
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class GameRuntimeLibraryAshP153Test {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase182() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shopVisitHook_appliesReplicaMrStoreYearSync() {
        CoinmasterDatabase.loadFromText(
            shopsText = "mrreplica\tReplica Mr. Store\n",
            coinText = "Replica Mr. Store\tbuy\t1\taugust scepter\tROW11325\n",
        )
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(
            preferences = p,
            character = KoLCharacter().apply {
                updateFromApiResponse(
                    CharacterApiResponse(
                        path = AscensionPath.LEGACY_OF_LOATHING.apiName,
                    ),
                )
            },
        )
        lib.processVisitResponseHooks(
            html = """<td colspan=14 align=center>&mdash; <b>2023</b> &mdash;</td>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mrreplica",
        )
        assertEquals(2023, p.getInt("currentReplicaStoreYear", 0))
    }

    @Test
    fun shopVisitHook_appliesBlackMarketMacguffinUnlock() {
        CoinmasterDatabase.loadFromText(
            shopsText = "blackmarket\tThe Black Market\n",
            coinText = "The Black Market\tROW290\tRed Zeppelin ticket\tpriceless diamond (1)\n",
        )
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(
            preferences = p,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(ascensions = "4"))
            },
        )
        lib.processVisitResponseHooks(
            html = "<html>The Black Market</html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=blackmarket",
        )
        assertEquals("step1", p.getString(Quest.MACGUFFIN.prefKey, UNSTARTED))
    }
}
