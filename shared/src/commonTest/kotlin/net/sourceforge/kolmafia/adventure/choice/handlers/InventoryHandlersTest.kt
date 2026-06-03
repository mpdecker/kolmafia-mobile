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
import net.sourceforge.kolmafia.character.MainStat
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

class InventoryHandlersTest {

    private val prefs = Preferences(MapSettings())
    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    private fun item(id: Int, qty: Int = 1) = id to InventoryItem(id, "item$id", qty, ItemType.OTHER)

    private fun ctx(
        choiceId: Int, preference: Int = 0,
        inventory: Map<Int, InventoryItem> = emptyMap(),
        charState: CharacterState = CharacterState(),
    ) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = "", characterState = charState,
        inventoryState = InventoryState(items = inventory),
        effectState = EffectState(), skillState = SkillState(),
        preferences = prefs, goalManager = GoalManager(),
        questDatabase = QuestDatabase(prefs), solvers = noOpSolvers,
        preference = preference,
    )

    // Case 5 — Heart of Very, Very Dark Darkness
    @Test fun case5_noRock_returns2() {
        val result = InventoryHandlers.handlers[5]?.decide(ctx(5))
        assertEquals(2, result)
    }

    @Test fun case5_hasRock_returns1() {
        val inv = mapOf(item(ItemPool.INEXPLICABLY_GLOWING_ROCK))
        val result = InventoryHandlers.handlers[5]?.decide(ctx(5, inventory = inv))
        assertEquals(1, result)
    }

    // Case 7 — How Depressing (fallback: no equipped)
    @Test fun case7_noGloveEquipped_returns2() {
        val result = InventoryHandlers.handlers[7]?.decide(ctx(7))
        assertEquals(2, result)
    }

    // Case 127 — No sir, away! A papaya war is on!
    @Test fun case127_pref4_enoughPapaya_returns2() {
        val inv = mapOf(item(ItemPool.PAPAYA, 3))
        val result = InventoryHandlers.handlers[127]?.decide(ctx(127, preference = 4, inventory = inv))
        assertEquals(2, result)
    }

    @Test fun case127_pref4_notEnoughPapaya_returns1() {
        val inv = mapOf(item(ItemPool.PAPAYA, 2))
        val result = InventoryHandlers.handlers[127]?.decide(ctx(127, preference = 4, inventory = inv))
        assertEquals(1, result)
    }

    @Test fun case127_pref5_enoughPapaya_returns2() {
        val inv = mapOf(item(ItemPool.PAPAYA, 3))
        val result = InventoryHandlers.handlers[127]?.decide(ctx(127, preference = 5, inventory = inv))
        assertEquals(2, result)
    }

    @Test fun case127_pref5_notEnoughPapaya_returns3() {
        val inv = mapOf(item(ItemPool.PAPAYA, 2))
        val result = InventoryHandlers.handlers[127]?.decide(ctx(127, preference = 5, inventory = inv))
        assertEquals(3, result)
    }

    @Test fun case127_pref3_returnsPreference() {
        val result = InventoryHandlers.handlers[127]?.decide(ctx(127, preference = 3))
        assertEquals(3, result)
    }

    // Case 161 — Bureaucracy of the Damned (Azazel)
    @Test fun case161_hasAllAzazelItems_returns1() {
        val inv = mapOf(
            item(ItemPool.AZAZEL_OBJECT_1),
            item(ItemPool.AZAZEL_OBJECT_2),
            item(ItemPool.AZAZEL_OBJECT_3),
        )
        val result = InventoryHandlers.handlers[161]?.decide(ctx(161, inventory = inv))
        assertEquals(1, result)
    }

    @Test fun case161_missingOneAzazelItem_returns4() {
        val inv = mapOf(
            item(ItemPool.AZAZEL_OBJECT_1),
            item(ItemPool.AZAZEL_OBJECT_2),
        )
        val result = InventoryHandlers.handlers[161]?.decide(ctx(161, inventory = inv))
        assertEquals(4, result)
    }

    // Case 191 — Chatterboxing
    @Test fun case191_pref5_hasTrinket_returns2() {
        val inv = mapOf(item(ItemPool.VALUABLE_TRINKET))
        val result = InventoryHandlers.handlers[191]?.decide(ctx(191, preference = 5, inventory = inv))
        assertEquals(2, result)
    }

    @Test fun case191_pref5_noTrinket_returns1() {
        val result = InventoryHandlers.handlers[191]?.decide(ctx(191, preference = 5))
        assertEquals(1, result)
    }

    // Case 298 — In the Shade
    @Test fun case298_pref1_missingItems_returns2() {
        val result = InventoryHandlers.handlers[298]?.decide(ctx(298, preference = 1))
        assertEquals(2, result)
    }

    @Test fun case298_pref1_hasAllItems_returnsPreference() {
        val inv = mapOf(item(ItemPool.SEED_PACKET), item(ItemPool.GREEN_SLIME))
        val result = InventoryHandlers.handlers[298]?.decide(ctx(298, preference = 1, inventory = inv))
        assertEquals(1, result)
    }

    // Case 305 — There is Sauce at the Bottom of the Ocean
    @Test fun case305_pref1_missingGlobe_returns2() {
        val result = InventoryHandlers.handlers[305]?.decide(ctx(305, preference = 1))
        assertEquals(2, result)
    }

    @Test fun case305_pref1_hasGlobe_returnsPreference() {
        val inv = mapOf(item(ItemPool.MERKIN_PRESSUREGLOBE))
        val result = InventoryHandlers.handlers[305]?.decide(ctx(305, preference = 1, inventory = inv))
        assertEquals(1, result)
    }

    // Case 504 — Tree's Last Stand
    @Test fun case504_noSapling_enoughMeat_templeNotUnlocked_returns3() {
        val charState = CharacterState(meat = 100)
        val prefs = Preferences(MapSettings())
        val ctx = ChoiceContext(
            choiceId = 504, options = mapOf(1 to "A", 2 to "B"),
            responseText = "", characterState = charState,
            inventoryState = InventoryState(), effectState = EffectState(),
            skillState = SkillState(), preferences = prefs,
            goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
            solvers = noOpSolvers, preference = 0,
        )
        val result = InventoryHandlers.handlers[504]?.decide(ctx)
        assertEquals(3, result)
    }

    @Test fun case504_hasSapling_returns4() {
        val inv = mapOf(item(ItemPool.SPOOKY_SAPLING))
        val result = InventoryHandlers.handlers[504]?.decide(ctx(504, inventory = inv))
        assertEquals(4, result)
    }

    // Case 553 — Relocked and Reloaded
    @Test fun case553_pref0_returnsNull() {
        val result = InventoryHandlers.handlers[553]?.decide(ctx(553, preference = 0))
        assertNull(result)
    }

    @Test fun case553_pref6_returns6() {
        val result = InventoryHandlers.handlers[553]?.decide(ctx(553, preference = 6))
        assertEquals(6, result)
    }

    @Test fun case553_pref1_hasHammer_returns1() {
        val inv = mapOf(item(ItemPool.MAXWELL_HAMMER))
        val result = InventoryHandlers.handlers[553]?.decide(ctx(553, preference = 1, inventory = inv))
        assertEquals(1, result)
    }

    @Test fun case553_pref1_noHammer_returns6() {
        val result = InventoryHandlers.handlers[553]?.decide(ctx(553, preference = 1))
        assertEquals(6, result)
    }

    // Case 558 — Tool Time
    @Test fun case558_pref2_enoughSticks_returns2() {
        val inv = mapOf(item(ItemPool.LOLLIPOP_STICK, 5))
        val result = InventoryHandlers.handlers[558]?.decide(ctx(558, preference = 2, inventory = inv))
        assertEquals(2, result)
    }

    @Test fun case558_pref2_notEnoughSticks_returns6() {
        val inv = mapOf(item(ItemPool.LOLLIPOP_STICK, 4))
        val result = InventoryHandlers.handlers[558]?.decide(ctx(558, preference = 2, inventory = inv))
        assertEquals(6, result)
    }

    // Case 692 — I Wanna Be a Door
    @Test fun case692_pref11_hasExpressCard_returns7() {
        val inv = mapOf(item(ItemPool.EXPRESS_CARD))
        val result = InventoryHandlers.handlers[692]?.decide(ctx(692, preference = 11, inventory = inv))
        assertEquals(7, result)
    }

    @Test fun case692_pref11_noItems_returnsNull() {
        val result = InventoryHandlers.handlers[692]?.decide(ctx(692, preference = 11))
        assertNull(result)
    }

    @Test fun case692_pref12_muscleHighest_returns4() {
        val charState = CharacterState(buffedMusc = 50, buffedMyst = 40, buffedMoxie = 35)
        val ctx = ChoiceContext(
            choiceId = 692, options = mapOf(1 to "A"),
            responseText = "", characterState = charState,
            inventoryState = InventoryState(), effectState = EffectState(),
            skillState = SkillState(), preferences = Preferences(MapSettings()),
            goalManager = GoalManager(), questDatabase = QuestDatabase(Preferences(MapSettings())),
            solvers = noOpSolvers, preference = 12,
        )
        val result = InventoryHandlers.handlers[692]?.decide(ctx)
        assertEquals(4, result)
    }

    @Test fun case692_pref12_mysticalityHighest_returns5() {
        val charState = CharacterState(buffedMusc = 40, buffedMyst = 50, buffedMoxie = 35)
        val ctx = ChoiceContext(
            choiceId = 692, options = mapOf(1 to "A"),
            responseText = "", characterState = charState,
            inventoryState = InventoryState(), effectState = EffectState(),
            skillState = SkillState(), preferences = Preferences(MapSettings()),
            goalManager = GoalManager(), questDatabase = QuestDatabase(Preferences(MapSettings())),
            solvers = noOpSolvers, preference = 12,
        )
        val result = InventoryHandlers.handlers[692]?.decide(ctx)
        assertEquals(5, result)
    }

    // Case 786 — Working Holiday (Hidden Office)
    @Test fun case786_pref1_noBinderClip_returns2() {
        val result = InventoryHandlers.handlers[786]?.decide(ctx(786, preference = 1))
        assertEquals(2, result)
    }

    @Test fun case786_pref2_returnsPreference() {
        val result = InventoryHandlers.handlers[786]?.decide(ctx(786, preference = 2))
        assertEquals(2, result)
    }

    // Case 791 — Legend of the Temple in the Hidden City
    @Test fun case791_pref1_notEnoughTriangles_returns6() {
        val inv = mapOf(item(ItemPool.STONE_TRIANGLE, 3))
        val result = InventoryHandlers.handlers[791]?.decide(ctx(791, preference = 1, inventory = inv))
        assertEquals(6, result)
    }

    @Test fun case791_pref1_enoughTriangles_returnsPreference() {
        val inv = mapOf(item(ItemPool.STONE_TRIANGLE, 4))
        val result = InventoryHandlers.handlers[791]?.decide(ctx(791, preference = 1, inventory = inv))
        assertEquals(1, result)
    }

    // Case 1091 — The Floor Is Yours (LavaCo)
    @Test fun case1091_pref1_missingRequiredItem_returnsNull() {
        val result = InventoryHandlers.handlers[1091]?.decide(ctx(1091, preference = 1))
        assertNull(result)
    }

    @Test fun case1091_pref1_hasRequiredItem_returnsPreference() {
        val inv = mapOf(item(ItemPool.GOLD_1970))
        val result = InventoryHandlers.handlers[1091]?.decide(ctx(1091, preference = 1, inventory = inv))
        assertEquals(1, result)
    }

    // Case 1489 — Slagging Off (Crimbo crystal)
    @Test fun case1489_noCrimboShardsz_returns3() {
        val result = InventoryHandlers.handlers[1489]?.decide(ctx(1489))
        assertEquals(3, result)
    }

    @Test fun case1489_pref1_hasShardsz_returns1() {
        val inv = mapOf(item(ItemPool.CRIMBO_CRYSTAL_SHARDS))
        val result = InventoryHandlers.handlers[1489]?.decide(ctx(1489, preference = 1, inventory = inv))
        assertEquals(1, result)
    }

    @Test fun case1489_prefOther_gobletsLessThanPlatters_returns1() {
        val inv = mapOf(
            item(ItemPool.CRIMBO_CRYSTAL_SHARDS),
            item(ItemPool.CRYSTAL_CRIMBO_GOBLET, 2),
            item(ItemPool.CRYSTAL_CRIMBO_PLATTER, 3),
        )
        val result = InventoryHandlers.handlers[1489]?.decide(ctx(1489, preference = 5, inventory = inv))
        assertEquals(1, result)
    }

    @Test fun case1489_prefOther_gobletsMoreThanPlatters_returns2() {
        val inv = mapOf(
            item(ItemPool.CRIMBO_CRYSTAL_SHARDS),
            item(ItemPool.CRYSTAL_CRIMBO_GOBLET, 3),
            item(ItemPool.CRYSTAL_CRIMBO_PLATTER, 2),
        )
        val result = InventoryHandlers.handlers[1489]?.decide(ctx(1489, preference = 5, inventory = inv))
        assertEquals(2, result)
    }
}
