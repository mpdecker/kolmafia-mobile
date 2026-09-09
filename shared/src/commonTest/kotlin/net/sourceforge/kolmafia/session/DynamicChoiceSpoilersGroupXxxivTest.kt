package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.sqrt
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.CharpaneStatusSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.FloristRequest
import net.sourceforge.kolmafia.servant.EdServantManager
import net.sourceforge.kolmafia.servant.EdServantRecord
import net.sourceforge.kolmafia.servant.EdServantState

/**
 * Group A+B tests — Overlook sidekick DI + Jarlsberg Eggman (Behavioral Deepen XXXIV).
 */
class DynamicChoiceSpoilersGroupXxxivTest {

    @BeforeTest
    fun setUp() {
        ModifierDatabase.resetForTest()
        resetOverlookDi()
    }

    @AfterTest
    fun tearDown() {
        resetOverlookDi()
        ModifierDatabase.resetForTest()
    }

    private fun resetOverlookDi() {
        DynamicChoiceSpoilers.hasActiveFamiliar = { false }
        DynamicChoiceSpoilers.activeFamiliarItemDrop = { 0.0 }
        DynamicChoiceSpoilers.activeFamiliarFoodDrop = { 0.0 }
        DynamicChoiceSpoilers.clancyLuteItemDrop = { 0.0 }
        DynamicChoiceSpoilers.eggmanItemDrop = { 0.0 }
        DynamicChoiceSpoilers.edCatServantItemDrop = { 0.0 }
        DynamicChoiceSpoilers.enthronedItemDrop = { 0.0 }
        DynamicChoiceSpoilers.bjornedItemDrop = { 0.0 }
        DynamicChoiceSpoilers.floristTwinPeakItemDrop = { 0.0 }
        DynamicChoiceSpoilers.characterClass = { null }
    }

    @Test
    fun clancyLuteItemDrop_nonZeroWhenLuteEquipped() {
        val bonus = DynamicChoiceSpoilers.computeClancyLuteItemDrop("lute", 7)
        val weight = 5 * 7
        val expected = sqrt(55.0 * weight) + weight - 3
        assertEquals(expected, bonus, 0.001)
        assertTrue(bonus > 0.0)
    }

    @Test
    fun clancyLuteItemDrop_zeroForNonLuteInstrument() {
        assertEquals(0.0, DynamicChoiceSpoilers.computeClancyLuteItemDrop("sackbut", 7), 0.001)
    }

    @Test
    fun edCatServantItemDrop_nonZeroAtLevelSeven() {
        val bonus = DynamicChoiceSpoilers.computeEdCatServantItemDrop("Cat", 7)
        val expected = sqrt(55.0 * 7) + 7 - 3
        assertEquals(expected, bonus, 0.001)
        assertTrue(bonus > 0.0)
    }

    @Test
    fun edCatServantItemDrop_zeroBelowLevelSeven() {
        assertEquals(0.0, DynamicChoiceSpoilers.computeEdCatServantItemDrop("Cat", 6), 0.001)
        assertEquals(0.0, DynamicChoiceSpoilers.computeEdCatServantItemDrop("Maid", 10), 0.001)
    }

    @Test
    fun eggmanItemDrop_workingLunchGate() {
        assertEquals(50.0, DynamicChoiceSpoilers.computeEggmanItemDrop(false), 0.001)
        assertEquals(75.0, DynamicChoiceSpoilers.computeEggmanItemDrop(true), 0.001)
    }

    @Test
    fun floristTwinPeakItemDrop_sumsPlantItemDrop() {
        ModifierDatabase.injectForTest("Florist", "Stealing Magnolia", "Item Drop: +25")
        ModifierDatabase.injectForTest("Florist", "Horn of Plenty", "Item Drop: +25")
        val bonus = DynamicChoiceSpoilers.computeFloristTwinPeakItemDrop(
            listOf(
                FloristRequest.Florist.STEALING_MAGNOLIA,
                FloristRequest.Florist.HORN_OF_PLENTY,
            ),
        )
        assertEquals(50.0, bonus, 0.001)
    }

    @Test
    fun overlookExclusionBonus_clancySidekickPath() {
        DynamicChoiceSpoilers.hasActiveFamiliar = { false }
        DynamicChoiceSpoilers.activeFamiliarItemDrop = { 0.0 }
        DynamicChoiceSpoilers.activeFamiliarFoodDrop = { 0.0 }
        DynamicChoiceSpoilers.clancyLuteItemDrop = {
            DynamicChoiceSpoilers.computeClancyLuteItemDrop("lute", 7)
        }
        DynamicChoiceSpoilers.eggmanItemDrop = { 0.0 }
        DynamicChoiceSpoilers.edCatServantItemDrop = { 0.0 }
        DynamicChoiceSpoilers.enthronedItemDrop = { 0.0 }
        DynamicChoiceSpoilers.bjornedItemDrop = { 0.0 }
        DynamicChoiceSpoilers.floristTwinPeakItemDrop = { 0.0 }

        val bonus = DynamicChoiceSpoilers.overlookItemDropExclusionBonus()
        assertTrue(bonus > 0.0)
    }

    @Test
    fun overlookExclusionBonus_eggmanSidekickPath() {
        DynamicChoiceSpoilers.hasActiveFamiliar = { false }
        DynamicChoiceSpoilers.activeFamiliarItemDrop = { 0.0 }
        DynamicChoiceSpoilers.activeFamiliarFoodDrop = { 0.0 }
        DynamicChoiceSpoilers.clancyLuteItemDrop = { 0.0 }
        DynamicChoiceSpoilers.eggmanItemDrop = { 75.0 }
        DynamicChoiceSpoilers.edCatServantItemDrop = { 0.0 }
        DynamicChoiceSpoilers.enthronedItemDrop = { 0.0 }
        DynamicChoiceSpoilers.bjornedItemDrop = { 0.0 }
        DynamicChoiceSpoilers.floristTwinPeakItemDrop = { 0.0 }

        assertEquals(75.0, DynamicChoiceSpoilers.overlookItemDropExclusionBonus(), 0.001)
    }

    @Test
    fun jarlsbergCompanion_parsedFromCharpane() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(challengePath = AscensionPath.AVATAR_OF_JARLSBERG.apiName)
        val html = """<b>Companion</b><br>the Eggman and his jarl_eggman.gif"""
        CharpaneStatusSync.checkJarlsbergCompanion(html, prefs, state)
        assertEquals("Eggman", prefs.getString(DynamicChoiceSpoilers.JARLSBERG_COMPANION_PREF, ""))
    }

    @Test
    fun jarlsbergCompanion_clearedWhenAbsent() {
        val prefs = Preferences(MapSettings())
        prefs.setString(DynamicChoiceSpoilers.JARLSBERG_COMPANION_PREF, "Eggman")
        val state = CharacterState(challengePath = AscensionPath.AVATAR_OF_JARLSBERG.apiName)
        CharpaneStatusSync.checkJarlsbergCompanion("<b>Companion</b><br>none", prefs, state)
        assertEquals("", prefs.getString(DynamicChoiceSpoilers.JARLSBERG_COMPANION_PREF, ""))
    }

    @Test
    fun edCatServantItemDrop_wiredFromManager() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(
                path = AscensionPath.ACTUALLY_ED_THE_UNDYING.apiName,
            ),
        )
        val manager = EdServantManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            prefs,
            char,
        )
        prefs.setString(EdServantManager.ACTIVE_SERVANT_PREF, "Cat")
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Whiskers", 7, 49))
        DynamicChoiceSpoilers.edCatServantItemDrop = {
            val record = manager.activeServantRecord()
            if (record == null) 0.0
            else DynamicChoiceSpoilers.computeEdCatServantItemDrop(record.type, record.level)
        }
        assertTrue(DynamicChoiceSpoilers.edCatServantItemDrop() > 0.0)
    }
}
