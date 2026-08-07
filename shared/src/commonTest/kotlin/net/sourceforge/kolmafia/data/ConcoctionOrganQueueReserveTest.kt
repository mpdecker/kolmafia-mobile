package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ConsumptionEligibility

class ConcoctionOrganQueueReserveTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun reserveFood_incrementsQueuedFullness() {
        registerConsumable("organ queue food", ConsumableType.FOOD, amount = 3)
        val delta = ConcoctionOrganQueueReserve.reserve("organ queue food", quantity = 2, ConcoctionQueueContext())
        assertEquals(6, ConcoctionQueueBudget.queuedFullness)
        assertEquals(6, delta.fullnessUsed)
        assertEquals(ConcoctionOrganAmounts.QueueBucket.FOOD, delta.queueBucket)
    }

    @Test
    fun reserveBooze_incrementsQueuedInebriety() {
        registerConsumable("organ queue booze", ConsumableType.DRINK, amount = 2)
        val delta = ConcoctionOrganQueueReserve.reserve("organ queue booze", quantity = 3, ConcoctionQueueContext())
        assertEquals(6, ConcoctionQueueBudget.queuedInebriety)
        assertEquals(6, delta.inebrietyUsed)
        assertEquals(ConcoctionOrganAmounts.QueueBucket.BOOZE, delta.queueBucket)
    }

    @Test
    fun reserveSpleen_incrementsQueuedSpleenHit() {
        registerConsumable("organ queue spleen", ConsumableType.SPLEEN, amount = 4)
        val delta = ConcoctionOrganQueueReserve.reserve("organ queue spleen", quantity = 2, ConcoctionQueueContext())
        assertEquals(8, ConcoctionQueueBudget.queuedSpleenHit)
        assertEquals(8, delta.spleenHitUsed)
        assertEquals(ConcoctionOrganAmounts.QueueBucket.SPLEEN, delta.queueBucket)
    }

    @Test
    fun release_reversesOrganCounters() {
        registerConsumable("organ queue release food", ConsumableType.FOOD, amount = 2)
        registerConsumable("organ queue release booze", ConsumableType.DRINK, amount = 1)
        registerConsumable("organ queue release spleen", ConsumableType.SPLEEN, amount = 3)

        val food = ConcoctionOrganQueueReserve.reserve("organ queue release food", 1, ConcoctionQueueContext())
        val booze = ConcoctionOrganQueueReserve.reserve("organ queue release booze", 2, ConcoctionQueueContext())
        val spleen = ConcoctionOrganQueueReserve.reserve("organ queue release spleen", 1, ConcoctionQueueContext())
        assertEquals(2, ConcoctionQueueBudget.queuedFullness)
        assertEquals(2, ConcoctionQueueBudget.queuedInebriety)
        assertEquals(3, ConcoctionQueueBudget.queuedSpleenHit)

        ConcoctionOrganQueueReserve.release(spleen, remainingReservations = emptyList())
        assertEquals(2, ConcoctionQueueBudget.queuedFullness)
        assertEquals(2, ConcoctionQueueBudget.queuedInebriety)
        assertEquals(0, ConcoctionQueueBudget.queuedSpleenHit)

        ConcoctionOrganQueueReserve.release(booze, remainingReservations = emptyList())
        assertEquals(2, ConcoctionQueueBudget.queuedFullness)
        assertEquals(0, ConcoctionQueueBudget.queuedInebriety)

        ConcoctionOrganQueueReserve.release(food, remainingReservations = emptyList())
        assertEquals(0, ConcoctionQueueBudget.queuedFullness)
    }

    @Test
    fun mimeShotglass_reducesInebrietyOnce() {
        registerConsumable("organ queue mime drink", ConsumableType.DRINK, amount = 1)
        registerItem(9676, "mime army shotglass")
        val context = ConcoctionQueueContext(
            availableCountById = { id -> if (id == 9676) 1 else 0 },
            getBooleanPref = { false },
        )

        val delta = ConcoctionOrganQueueReserve.reserve("organ queue mime drink", 1, context)
        assertEquals(0, ConcoctionQueueBudget.queuedInebriety)
        assertTrue(ConcoctionQueueBudget.queuedMimeShotglass)
        assertTrue(delta.mimeShotglassUsed)
        assertEquals(0, delta.inebrietyUsed)

        ConcoctionOrganQueueReserve.release(delta, remainingReservations = emptyList())
        assertEquals(0, ConcoctionQueueBudget.queuedInebriety)
        assertFalse(ConcoctionQueueBudget.queuedMimeShotglass)
    }

    @Test
    fun craftQueue_popRestoresOrganCounters() {
        registerConsumable("organ queue craft food", ConsumableType.FOOD, amount = 1)
        registerItem(8201, "organ queue craft food")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "organ queue craft food",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            "organ queue craft food",
            ConcoctionRuntimeState(initial = 0, creatable = 1),
        )

        val context = ConcoctionQueueContext(
            runtimeFor = { name ->
                if (name.equals("organ queue craft food", ignoreCase = true)) {
                    ConcoctionDatabase.getRuntime(name)
                } else {
                    null
                }
            },
        )
        assertTrue(ConcoctionCraftQueue.push("organ queue craft food", 1, context))
        assertEquals(1, ConcoctionQueueBudget.queuedFullness)

        ConcoctionCraftQueue.pop()
        assertEquals(0, ConcoctionQueueBudget.queuedFullness)
    }

    @Test
    fun effectiveFullnessRemaining_subtractsQueuedFood() {
        registerConsumable("organ queue effective food", ConsumableType.FOOD, amount = 4)
        ConcoctionOrganQueueReserve.reserve("organ queue effective food", 2, ConcoctionQueueContext())

        val state = CharacterState(challengePath = "Standard", fullness = 3)
        assertEquals(4, ConsumptionEligibility.effectiveFullnessRemaining(state))
        assertEquals(15, ConsumptionEligibility.stomachCapacity(state))
    }

    private fun registerConsumable(name: String, type: ConsumableType, amount: Int) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
                type = type,
                amount = amount,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 0,
                advMax = 0,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
    }
}
