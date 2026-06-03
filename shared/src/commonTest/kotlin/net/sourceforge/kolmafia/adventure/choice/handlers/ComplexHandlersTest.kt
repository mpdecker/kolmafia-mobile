package net.sourceforge.kolmafia.adventure.choice.handlers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
import net.sourceforge.kolmafia.adventure.choice.ChoiceSolvers
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.adventure.choice.solvers.ArcadeGameSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.GameproSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LightsOutSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LostKeySolver
import net.sourceforge.kolmafia.adventure.choice.solvers.SafetyShelterSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.VampOutSolver
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComplexHandlersTest {

    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    /** Fresh isolated preferences per call — no cross-test state leakage. */
    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun item(id: Int, qty: Int = 1) = id to InventoryItem(id, "item$id", qty, ItemType.OTHER)

    private fun ctx(
        choiceId: Int,
        preference: Int = 0,
        response: String = "",
        inventory: Map<Int, InventoryItem> = emptyMap(),
        charState: CharacterState = CharacterState(),
        prefs: Preferences = prefs(),
    ) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = response, characterState = charState,
        inventoryState = InventoryState(items = inventory),
        effectState = EffectState(), skillState = SkillState(),
        preferences = prefs, goalManager = GoalManager(),
        questDatabase = QuestDatabase(prefs), solvers = noOpSolvers,
        preference = preference,
    )

    private fun decide(
        choiceId: Int,
        preference: Int = 0,
        response: String = "",
        inventory: Map<Int, InventoryItem> = emptyMap(),
        charState: CharacterState = CharacterState(),
        prefs: Preferences = prefs(),
    ) = ComplexHandlers.handlers[choiceId]?.decide(
        ctx(choiceId, preference, response, inventory, charState, prefs)
    )

    // Case 304
    @Test fun case304_pref1_maxTempura_returns2() =
        assertEquals(2, decide(304, 1, prefs = prefs { setInt("tempuraSummons", 3) }))

    @Test fun case304_pref1_lowMp_returns2() =
        assertEquals(2, decide(304, 1,
            charState = CharacterState(currentMp = 100),
            prefs = prefs { setInt("tempuraSummons", 0) }))

    @Test fun case304_pref1_normal_returnsPref() =
        assertEquals(1, decide(304, 1,
            charState = CharacterState(currentMp = 300),
            prefs = prefs { setInt("tempuraSummons", 0) }))

    @Test fun case304_pref2_returnsPreference() = assertEquals(2, decide(304, 2))

    // Case 309
    @Test fun case309_pref1_seaodesMax_returns2() =
        assertEquals(2, decide(309, 1, prefs = prefs { setInt("seaodesFound", 3) }))

    @Test fun case309_pref1_normal_returnsPref() =
        assertEquals(1, decide(309, 1, prefs = prefs { setInt("seaodesFound", 0) }))

    @Test fun case309_pref0_returnsNull() = assertNull(decide(309, 0))

    // Case 496 — with no modifiers, hot damage = 0 < 20
    @Test fun case496_pref2_noHotDmg_returns1() = assertEquals(1, decide(496, 2))
    @Test fun case496_pref1_returnsPreference() = assertEquals(1, decide(496, 1))
    @Test fun case496_pref0_returnsNull() = assertNull(decide(496, 0))

    // Case 502
    @Test fun case502_pref2_choiceAdv505Is2_hasCoin_returns3() {
        val inv = mapOf(item(ItemPool.TREE_HOLED_COIN))
        assertEquals(3, decide(502, 2, inventory = inv, prefs = prefs { setString("choiceAdventure505", "2") }))
    }
    @Test fun case502_pref2_choiceAdv505Is2_noCoin_returnsPref() =
        assertEquals(2, decide(502, 2, prefs = prefs { setString("choiceAdventure505", "2") }))

    @Test fun case502_pref2_choiceAdv505IsNot2_returnsPref() =
        assertEquals(2, decide(502, 2, prefs = prefs { setString("choiceAdventure505", "1") }))

    // Cases 513–515 — with no modifiers, damage = 0 < 20
    @Test fun case513_pref2_noColdDmg_returns1() = assertEquals(1, decide(513, 2))
    @Test fun case514_pref2_noStenchDmg_returns1() = assertEquals(1, decide(514, 2))
    @Test fun case515_pref2_noSpookyDmg_returns1() = assertEquals(1, decide(515, 2))

    // Case 549
    @Test fun case549_pref0_returnsNull() = assertNull(decide(549, 0))
    @Test fun case549_pref1_returns1() = assertEquals(1, decide(549, 1))
    @Test fun case549_pref2_returns2() = assertEquals(2, decide(549, 2))
    @Test fun case549_pref3_returns5() = assertEquals(5, decide(549, 3))
    @Test fun case549_pref4_boomboxOff_returns3() = assertEquals(3, decide(549, 4, "no music"))
    @Test fun case549_pref4_boomboxOn_returns1() =
        assertEquals(1, decide(549, 4, "sets your heart pounding and pulse racing"))
    @Test fun case549_pref5_boomboxOff_returns3() = assertEquals(3, decide(549, 5, "no music"))
    @Test fun case549_pref5_boomboxOn_returns2() =
        assertEquals(2, decide(549, 5, "sets your heart pounding and pulse racing"))
    @Test fun case549_pref6_boomboxOff_returns3() = assertEquals(3, decide(549, 6, "no music"))
    @Test fun case549_pref6_boomboxOn_returns5() =
        assertEquals(5, decide(549, 6, "sets your heart pounding and pulse racing"))
    @Test fun case549_pref7_boomboxOn_hasShotgun_returns5() {
        val inv = mapOf(item(ItemPool.SILVER_SHOTGUN_SHELL))
        assertEquals(5, decide(549, 7, "sets your heart pounding and pulse racing", inv))
    }
    @Test fun case549_pref7_boomboxOn_noShotgun_returns2() =
        assertEquals(2, decide(549, 7, "sets your heart pounding and pulse racing"))
    @Test fun case549_pref8_boomboxOn_returns4() =
        assertEquals(4, decide(549, 8, "sets your heart pounding and pulse racing"))
    @Test fun case549_pref8_boomboxOff_returns1() = assertEquals(1, decide(549, 8, "silence"))
    @Test fun case549_pref9_boomboxOn_returns4() =
        assertEquals(4, decide(549, 9, "sets your heart pounding and pulse racing"))
    @Test fun case549_pref9_boomboxOff_returns2() = assertEquals(2, decide(549, 9, "silence"))
    @Test fun case549_pref10_boomboxOn_returns4() =
        assertEquals(4, decide(549, 10, "sets your heart pounding and pulse racing"))
    @Test fun case549_pref10_boomboxOff_returns5() = assertEquals(5, decide(549, 10, "silence"))
    @Test fun case549_pref11_boomboxOn_hasShotgun_returns4() {
        val inv = mapOf(item(ItemPool.SILVER_SHOTGUN_SHELL))
        assertEquals(4, decide(549, 11, "sets your heart pounding and pulse racing", inv))
    }
    @Test fun case549_pref11_boomboxOff_noShotgun_returns2() =
        assertEquals(2, decide(549, 11, "silence"))

    // Case 550
    @Test fun case550_pref0_returnsNull() = assertNull(decide(550, 0))
    @Test fun case550_pref1_returns3() = assertEquals(3, decide(550, 1))
    @Test fun case550_pref2_returns4() = assertEquals(4, decide(550, 2))
    @Test fun case550_pref3_returns5() = assertEquals(5, decide(550, 3))
    @Test fun case550_pref4_notClosed_returns1() = assertEquals(1, decide(550, 4, "open windows"))
    @Test fun case550_pref4_closed_returns3() = assertEquals(3, decide(550, 4, "covered all their windows"))
    @Test fun case550_pref5_notClosed_returns1() = assertEquals(1, decide(550, 5, "open windows"))
    @Test fun case550_pref5_closed_returns4() = assertEquals(4, decide(550, 5, "covered all their windows"))
    @Test fun case550_pref6_closed_chainsawMore_returns3() {
        val inv = mapOf(item(ItemPool.CHAINSAW_CHAIN, 2), item(ItemPool.FUNHOUSE_MIRROR, 1))
        assertEquals(3, decide(550, 6, "covered all their windows", inv))
    }
    @Test fun case550_pref6_closed_mirrorMore_returns4() {
        val inv = mapOf(item(ItemPool.CHAINSAW_CHAIN, 1), item(ItemPool.FUNHOUSE_MIRROR, 2))
        assertEquals(4, decide(550, 6, "covered all their windows", inv))
    }
    @Test fun case550_pref6_notClosed_returns1() = assertEquals(1, decide(550, 6, "open"))
    @Test fun case550_pref7_notClosed_returns1() = assertEquals(1, decide(550, 7, "open"))
    @Test fun case550_pref7_closed_returns5() = assertEquals(5, decide(550, 7, "covered all their windows"))
    @Test fun case550_pref8_closed_returns2() = assertEquals(2, decide(550, 8, "covered all their windows"))
    @Test fun case550_pref8_notClosed_returns3() = assertEquals(3, decide(550, 8, "open"))
    @Test fun case550_pref9_closed_returns2() = assertEquals(2, decide(550, 9, "covered all their windows"))
    @Test fun case550_pref9_notClosed_returns4() = assertEquals(4, decide(550, 9, "open"))
    @Test fun case550_pref10_notClosed_chainsawMore_returns3() {
        val inv = mapOf(item(ItemPool.CHAINSAW_CHAIN, 2), item(ItemPool.FUNHOUSE_MIRROR, 1))
        assertEquals(3, decide(550, 10, "open", inv))
    }
    @Test fun case550_pref11_closed_returns2() = assertEquals(2, decide(550, 11, "covered all their windows"))
    @Test fun case550_pref11_notClosed_returns5() = assertEquals(5, decide(550, 11, "open"))

    // Case 551
    @Test fun case551_pref1_returnsPref() = assertEquals(1, decide(551, 1))
    @Test fun case551_pref2_returnsPref() = assertEquals(2, decide(551, 2))
    @Test fun case551_pref3_fogOn_returns1() =
        assertEquals(1, decide(551, 3, "white clouds of artificial fog"))
    @Test fun case551_pref3_fogOff_returns3() = assertEquals(3, decide(551, 3, "no fog"))
    @Test fun case551_pref4_fogOn_returns2() =
        assertEquals(2, decide(551, 4, "white clouds of artificial fog"))
    @Test fun case551_pref4_fogOff_returns3() = assertEquals(3, decide(551, 4, "no fog"))
    @Test fun case551_pref5_fogOn_returns4() =
        assertEquals(4, decide(551, 5, "white clouds of artificial fog"))
    @Test fun case551_pref5_fogOff_returns1() = assertEquals(1, decide(551, 5, "no fog"))
    @Test fun case551_pref6_fogOn_returns4() =
        assertEquals(4, decide(551, 6, "white clouds of artificial fog"))
    @Test fun case551_pref6_fogOff_returns2() = assertEquals(2, decide(551, 6, "no fog"))

    // Case 552
    @Test fun case552_pref4_chainsawLess_returns1() {
        val inv = mapOf(item(ItemPool.CHAINSAW_CHAIN, 1), item(ItemPool.FUNHOUSE_MIRROR, 2))
        assertEquals(1, decide(552, 4, inventory = inv))
    }
    @Test fun case552_pref4_chainsawMore_returns3() {
        val inv = mapOf(item(ItemPool.CHAINSAW_CHAIN, 2), item(ItemPool.FUNHOUSE_MIRROR, 1))
        assertEquals(3, decide(552, 4, inventory = inv))
    }
    @Test fun case552_pref4_chainsawEqual_returns3() {
        val inv = mapOf(item(ItemPool.CHAINSAW_CHAIN, 2), item(ItemPool.FUNHOUSE_MIRROR, 2))
        assertEquals(3, decide(552, 4, inventory = inv))  // chainsaw < mirror is false when equal
    }
    @Test fun case552_pref2_returnsPref() = assertEquals(2, decide(552, 2))
}
