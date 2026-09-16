package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ConcoctionBuyables
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.RichardRequest

class ClanDungeonPhase7450Test {

    @AfterTest
    fun tearDown() {
        HobopolisManager.consumePendingStop()
        ModifierDatabase.resetForTest()
    }

    @Test
    fun slimeStackTurns_andFeedAndLeaps() {
        val prefs = Preferences(MapSettings())
        assertEquals(1, SlimeStackManager.getSlimeStackTurns(1))
        assertEquals(6, SlimeStackManager.getSlimeStackTurns(3))
        assertTrue(SlimeStackManager.isMeatStackFeed(ItemPool.GNOLLISH_AUTOPLUNGER))
        assertTrue(SlimeStackManager.isMeatStackFeed(ConcoctionBuyables.MEAT_PASTE))
        SlimeStackManager.recordFeed(ItemPool.GNOLLISH_AUTOPLUNGER, 2, prefs)
        assertEquals(2, prefs.getInt(SlimeStackManager.STACKS_DUE_PREF, 0))
        prefs.setFloat(SlimeStackManager.FULLNESS_PREF, 3f)
        assertTrue(SlimeStackManager.recordFightLeaps("Gooey leaps on your opponent, sliming it for 12 damage.", prefs))
        assertEquals(2f, prefs.getFloat(SlimeStackManager.FULLNESS_PREF, 0f))
        assertTrue(SlimeStackManager.recordStackDrop(prefs, SlimeStackManager.SLIMELING_FAMILIAR_ID))
        assertEquals(1, prefs.getInt(SlimeStackManager.STACKS_DROPPED_PREF, 0))
        assertFalse(SlimeStackManager.recordStackDrop(prefs, 1))
    }

    @Test
    fun motherSlimeWait_recordsAbortStop() {
        assertEquals("Mother Slime waits for you.", SlimeTubeManager.motherSlimeWait(326, 2))
        assertNull(SlimeTubeManager.motherSlimeWait(326, 1))
        assertTrue(SlimeTubeManager.postChoice(326, 2))
        assertEquals("Mother Slime waits for you.", HobopolisManager.consumePendingStop())
    }

    @Test
    fun richardGymNames_andSessionLog() {
        assertEquals(
            "Help Richard make grenades (Moxie)",
            RichardRequest.gymType("clan_hobopolis.php?place=3&preaction=spendturns&whichservice=2"),
        )
        assertEquals(
            "Help Richard make shakes (Muscle)",
            RichardRequest.gymType("clan_hobopolis.php?place=3&preaction=spendturns&whichservice=3"),
        )
        assertTrue(
            RichardRequest.registerRequest(
                "clan_hobopolis.php?place=3&preaction=spendturns&whichservice=1&numturns=4",
            ),
        )
        assertEquals(4, RichardRequest.getAdventuresUsed("clan_hobopolis.php?preaction=spendturns&numturns=4"))
    }

    @Test
    fun slimeHatred_addsMonsterLevelInSlimeTube() {
        ModifierDatabase.injectForTest("Item", "Slime Hate Hat", "Slime Hates It: +2")
        val prefs = Preferences(MapSettings())
        prefs.setString("lastAdventure", "The Slime Tube")
        val mods = CurrentModifiers(
            CharacterState(equipment = mapOf(EquipmentSlot.HAT to "Slime Hate Hat")),
            preferences = prefs,
        )
        assertEquals(2.0, mods.values.get(DoubleModifier.SLIME_HATES_IT))
        assertEquals(120.0, mods.values.get(DoubleModifier.MONSTER_LEVEL))
    }

    @Test
    fun slimeHatred_skippedOutsideSlimeTube() {
        ModifierDatabase.injectForTest("Item", "Slime Hate Hat", "Slime Hates It: +2")
        val prefs = Preferences(MapSettings())
        prefs.setString("lastAdventure", "The Haunted Pantry")
        val mods = CurrentModifiers(
            CharacterState(equipment = mapOf(EquipmentSlot.HAT to "Slime Hate Hat")),
            preferences = prefs,
        )
        assertEquals(2.0, mods.values.get(DoubleModifier.SLIME_HATES_IT))
        assertEquals(0.0, mods.values.get(DoubleModifier.MONSTER_LEVEL))
    }

    @Test
    fun sewerPrep_blocksWhenRequiredItemsMissing() = kotlinx.coroutines.runBlocking {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(HobopolisManager.REQUIRE_SEWER_TEST_ITEMS, true)
        val ctx = net.sourceforge.kolmafia.adventure.prep.AdventureGateContext(preferences = prefs)
        val ok = net.sourceforge.kolmafia.adventure.prep.AdventurePrepareActions.prepare(
            HobopolisManager.SEWER_LOCATION,
            zone = null,
            ctx = ctx,
            deps = net.sourceforge.kolmafia.adventure.prep.AdventurePrepareActions.PrepareDeps(
                outfitManager = null,
                retrieveItemService = null,
                useItemRequest = null,
                gameDatabase = null,
            ),
        )
        assertFalse(ok)
        prefs.setBoolean(HobopolisManager.REQUIRE_SEWER_TEST_ITEMS, false)
        val okWhenOff = net.sourceforge.kolmafia.adventure.prep.AdventurePrepareActions.prepare(
            HobopolisManager.SEWER_LOCATION,
            zone = null,
            ctx = net.sourceforge.kolmafia.adventure.prep.AdventureGateContext(preferences = prefs),
            deps = net.sourceforge.kolmafia.adventure.prep.AdventurePrepareActions.PrepareDeps(
                outfitManager = null,
                retrieveItemService = null,
                useItemRequest = null,
                gameDatabase = null,
            ),
        )
        assertTrue(okWhenOff)
    }

    @Test
    fun dreadsylvaniaFeedbooze_recordsLastBooze() {
        val prefs = Preferences(MapSettings())
        net.sourceforge.kolmafia.request.DreadsylvaniaRequest.parseResponse(
            "clan_dreadsylvania.php?action=feedbooze&whichbooze=81&boozequantity=2",
            "ok",
            prefs,
        )
        assertEquals(81, prefs.getInt("_dreadLastBooze", 0))
        assertEquals(2, prefs.getInt("_dreadLastBoozeQty", 0))
    }
}
