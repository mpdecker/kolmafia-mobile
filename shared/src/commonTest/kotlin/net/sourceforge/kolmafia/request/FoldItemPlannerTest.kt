package net.sourceforge.kolmafia.request

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType
import com.russhwolf.settings.MapSettings

class FoldItemPlannerTest {

    @AfterTest
    fun tearDown() {
        FoldGroupDatabase.resetForTest()
    }

    private fun ctx(
        inventory: Map<Int, Int> = emptyMap(),
        equipped: Map<String, EquipmentSlot> = emptyMap(),
        accessible: Map<Int, Int> = inventory,
        skills: List<SkillData> = emptyList(),
        state: CharacterState = CharacterState(),
        prefs: Preferences? = null,
        shirts: Set<Int> = emptySet(),
        chefStaffs: Set<String> = emptySet(),
        names: Map<Int, String>,
    ): FoldItemPlanner.Context {
        val byName = names.entries.associate { it.value.lowercase() to it.key }
        return FoldItemPlanner.Context(
            inventoryCount = { inventory[it] ?: 0 },
            equippedSlot = { name -> equipped.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value },
            accessibleCount = { accessible[it] ?: 0 },
            skills = skills,
            charState = state,
            preferences = prefs,
            itemId = { byName[it.lowercase()] },
            itemName = { names[it] },
            isShirt = { it in shirts },
            isChefStaff = { it in chefStaffs },
        )
    }

    @Test
    fun alreadyHave_skipsFold() {
        FoldGroupDatabase.registerGroupForTest(FoldGroup(0, listOf("fold-a", "fold-b")))
        val names = mapOf(1 to "fold-a", 2 to "fold-b")
        val plan = FoldItemPlanner.plan(1, ctx(inventory = mapOf(1 to 1), names = names))
        assertTrue(plan.alreadyHave)
    }

    @Test
    fun unknownItem_errors() {
        val plan = FoldItemPlanner.plan(99, ctx(names = mapOf(99 to "not foldable")))
        assertEquals("That's not a transformable item!", plan.error)
    }

    @Test
    fun shirtWithoutTorso_errors() {
        FoldGroupDatabase.registerGroupForTest(FoldGroup(0, listOf("fold-shirt-a", "fold-shirt-b")))
        val names = mapOf(10 to "fold-shirt-a", 11 to "fold-shirt-b")
        val plan = FoldItemPlanner.plan(
            10,
            ctx(inventory = mapOf(11 to 1), names = names, shirts = setOf(10)),
        )
        assertEquals("You can't make a shirt", plan.error)
    }

    @Test
    fun walksBackwardToInventoryPeer() {
        FoldGroupDatabase.registerGroupForTest(FoldGroup(5, listOf("fold-a", "fold-b", "fold-c")))
        val names = mapOf(1 to "fold-a", 2 to "fold-b", 3 to "fold-c")
        val plan = FoldItemPlanner.plan(1, ctx(inventory = mapOf(2 to 1), names = names))
        assertNull(plan.error)
        assertEquals(listOf(2, 3), plan.useItemIds)
    }

    @Test
    fun garbageZeroCharge_refoldsEvenIfOwned() {
        FoldGroupDatabase.registerGroupForTest(
            FoldGroup(0, listOf("January's Garbage Tote", "broken champagne bottle")),
        )
        val names = mapOf(
            FoldItemPlanner.GARBAGE_TOTE to "January's Garbage Tote",
            FoldItemPlanner.BROKEN_CHAMPAGNE to "broken champagne bottle",
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("garbageChampagneCharge", 0)
        val plan = FoldItemPlanner.plan(
            FoldItemPlanner.BROKEN_CHAMPAGNE,
            ctx(
                inventory = mapOf(FoldItemPlanner.BROKEN_CHAMPAGNE to 1, FoldItemPlanner.GARBAGE_TOTE to 1),
                names = names,
                prefs = prefs,
            ),
        )
        assertEquals(FoldItemPlanner.Special.GARBAGE_TOTE, plan.special)
        assertEquals(FoldItemPlanner.GARBAGE_TOTE, plan.toteItemId)
    }

    @Test
    fun borisHelmEquipped_twists() {
        FoldGroupDatabase.registerGroupForTest(FoldGroup(0, listOf("Boris's Helm", "Boris's Helm (askew)")))
        val names = mapOf(20 to "Boris's Helm", 21 to "Boris's Helm (askew)")
        val plan = FoldItemPlanner.plan(
            21,
            ctx(
                equipped = mapOf("Boris's Helm (askew)" to EquipmentSlot.HAT),
                names = names,
            ),
        )
        assertEquals(FoldItemPlanner.Special.BORIS_HELM, plan.special)
        assertEquals(EquipmentSlot.HAT, plan.unequipSlot)
    }

    @Test
    fun chefStaffWithoutSkill_errors() {
        FoldGroupDatabase.registerGroupForTest(FoldGroup(0, listOf("staff-a", "staff-b")))
        val names = mapOf(30 to "staff-a", 31 to "staff-b")
        val plan = FoldItemPlanner.plan(
            30,
            ctx(inventory = mapOf(31 to 1), names = names, chefStaffs = setOf("staff-a")),
        )
        assertEquals("You can't make a chefstaff", plan.error)
    }

    @Test
    fun chefStaffWithRigatoni_ok() {
        FoldGroupDatabase.registerGroupForTest(FoldGroup(0, listOf("staff-a", "staff-b")))
        val names = mapOf(30 to "staff-a", 31 to "staff-b")
        val plan = FoldItemPlanner.plan(
            30,
            ctx(
                inventory = mapOf(31 to 1),
                names = names,
                chefStaffs = setOf("staff-a"),
                skills = listOf(
                    SkillData(
                        id = FoldItemPlanner.SPIRIT_OF_RIGATONI,
                        name = "Spirit of Rigatoni",
                        type = SkillType.PASSIVE,
                        mpCost = 0,
                        dailyLimit = 0,
                        timesCast = 0,
                    ),
                ),
            ),
        )
        assertNull(plan.error)
        assertEquals(listOf(31), plan.useItemIds)
    }
}
