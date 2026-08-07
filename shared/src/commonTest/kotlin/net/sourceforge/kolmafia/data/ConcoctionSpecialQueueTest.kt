package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class ConcoctionSpecialQueueTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        DailyLimitDatabase.resetForTest()
        ConcoctionCraftQueue.resetForTest()
        ConcoctionQueueBudget.resetForTest()
    }

    @Test
    fun reserveSmores_incrementsCounterAndRefreshesConsumable() {
        registerItem(ConcoctionSpecialQueue.SMORE, "s'more")
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "s'more",
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.CRAPPY,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("smoresEaten", 2)
        val context = ConcoctionQueueContext(preferences = prefs)

        val delta = ConcoctionSpecialQueue.reserve("s'more", quantity = 3, context)

        assertEquals(1, ConcoctionQueueBudget.queuedSmores)
        assertEquals(1, delta.smoresUsed)
        assertTrue(delta.refreshedSmoresData)
        val size = 3
        val expectedAdv = ceil(size.toDouble().pow(1.75)).toInt()
        assertEquals(size, ConsumableDatabase.getFood("s'more")?.amount)
        assertEquals(expectedAdv.toString(), ConsumableDatabase.getAdventureRange("s'more"))
    }

    @Test
    fun releaseSmores_reversesCounterAndAdjustsFullness() {
        registerItem(ConcoctionSpecialQueue.SMORE, "s'more")
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "s'more",
                type = ConsumableType.FOOD,
                amount = 2,
                levelReq = 1,
                quality = ConsumableQuality.CRAPPY,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "s'more",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("smoresEaten", 1)
        val context = ConcoctionQueueContext(
            preferences = prefs,
            runtimeFor = { name ->
                if (name.equals("s'more", ignoreCase = true)) {
                    ConcoctionRuntimeState(initial = 1, creatable = 0)
                } else {
                    null
                }
            },
        )

        assertTrue(ConcoctionCraftQueue.push("s'more", 1, context))
        assertEquals(1, ConcoctionQueueBudget.queuedSmores)
        assertEquals(2, ConcoctionQueueBudget.queuedFullness)

        ConcoctionCraftQueue.pop()
        assertEquals(0, ConcoctionQueueBudget.queuedSmores)
        assertEquals(1, ConcoctionQueueBudget.queuedFullness)
    }

    @Test
    fun reserveSpeakeasyDrink_incrementsByQuantity() {
        registerItem(9001, "speakeasy test drink")
        DailyLimitDatabase.registerEntryForTest(
            DailyLimitEntry(
                kind = DailyLimitKind.DRINK,
                itemId = 9001,
                trackingProperty = "_speakeasyDrinksDrunk",
                maxValue = 3,
            ),
        )

        val delta = ConcoctionSpecialQueue.reserve("speakeasy test drink", quantity = 2, ConcoctionQueueContext())

        assertEquals(2, ConcoctionQueueBudget.queuedSpeakeasyDrink)
        assertEquals(2, delta.speakeasyDrinkUsed)
    }

    @Test
    fun reserveFancyHotDog_setsQueuedFancyDog() {
        val delta = ConcoctionSpecialQueue.reserve("sly dog", quantity = 1, ConcoctionQueueContext())

        assertTrue(ConcoctionQueueBudget.queuedFancyDog)
        assertTrue(delta.fancyDogUsed)
    }

    @Test
    fun reserveBasicHotDog_doesNotSetQueuedFancyDog() {
        val delta = ConcoctionSpecialQueue.reserve("basic hot dog", quantity = 1, ConcoctionQueueContext())

        assertFalse(ConcoctionQueueBudget.queuedFancyDog)
        assertFalse(delta.fancyDogUsed)
    }

    @Test
    fun pushPopFancyHotDog_clearsQueuedFancyDog() {
        registerItem(9002, "sly dog")
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "sly dog",
                type = ConsumableType.FOOD,
                amount = 2,
                levelReq = 1,
                quality = ConsumableQuality.AWESOME,
                advMin = 6,
                advMax = 8,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 10,
                moxieMax = 15,
                notes = "",
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "sly dog",
                resultQuantity = 1,
                methods = setOf("HOT_DOG"),
                ingredients = emptyList(),
            ),
        )
        val context = ConcoctionQueueContext(
            runtimeFor = { name ->
                if (name.equals("sly dog", ignoreCase = true)) {
                    ConcoctionRuntimeState(initial = 1, creatable = 0)
                } else {
                    null
                }
            },
        )

        assertTrue(ConcoctionCraftQueue.push("sly dog", 1, context))
        assertTrue(ConcoctionQueueBudget.queuedFancyDog)
        assertEquals(2, ConcoctionQueueBudget.queuedFullness)

        ConcoctionCraftQueue.pop()
        assertFalse(ConcoctionQueueBudget.queuedFancyDog)
        assertEquals(0, ConcoctionQueueBudget.queuedFullness)
    }

    @Test
    fun pushFancyHotDog_blockedWhenAlreadyEaten() {
        registerItem(9002, "sly dog")
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "sly dog",
                type = ConsumableType.FOOD,
                amount = 2,
                levelReq = 1,
                quality = ConsumableQuality.AWESOME,
                advMin = 6,
                advMax = 8,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 10,
                moxieMax = 15,
                notes = "",
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "sly dog",
                resultQuantity = 1,
                methods = setOf("HOT_DOG"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_fancyHotDogEaten", true)
        val context = ConcoctionQueueContext(
            preferences = prefs,
            runtimeFor = { name ->
                if (name.equals("sly dog", ignoreCase = true)) {
                    ConcoctionRuntimeState(initial = 1, creatable = 0)
                } else {
                    null
                }
            },
        )

        assertFalse(ConcoctionCraftQueue.push("sly dog", 1, context))
        assertFalse(ConcoctionQueueBudget.queuedFancyDog)
        assertEquals(0, ConcoctionQueueBudget.queuedFullness)
    }

    @Test
    fun pushSpeakeasyDrink_blockedWhenDailyCapReached() {
        registerSpeakeasyDrink(7592, "Lucky Lindy")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "Lucky Lindy",
                resultQuantity = 1,
                methods = setOf("SPEAKEASY"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("_speakeasyDrinksDrunk", 3)
        val context = ConcoctionQueueContext(
            preferences = prefs,
            runtimeFor = { name ->
                if (name.equals("Lucky Lindy", ignoreCase = true)) {
                    ConcoctionRuntimeState(initial = 0, creatable = 1)
                } else {
                    null
                }
            },
        )

        assertFalse(ConcoctionCraftQueue.push("Lucky Lindy", 1, context))
        assertEquals(0, ConcoctionQueueBudget.queuedSpeakeasyDrink)
    }

    @Test
    fun pushSpeakeasyDrink_blockedWhenQueuedWouldExceedCap() {
        registerSpeakeasyDrink(7592, "Lucky Lindy")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "Lucky Lindy",
                resultQuantity = 1,
                methods = setOf("SPEAKEASY"),
                ingredients = emptyList(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("_speakeasyDrinksDrunk", 2)
        val context = ConcoctionQueueContext(
            preferences = prefs,
            runtimeFor = { name ->
                if (name.equals("Lucky Lindy", ignoreCase = true)) {
                    ConcoctionRuntimeState(initial = 0, creatable = 1)
                } else {
                    null
                }
            },
        )

        assertTrue(ConcoctionCraftQueue.push("Lucky Lindy", 1, context))
        assertEquals(1, ConcoctionQueueBudget.queuedSpeakeasyDrink)
        assertFalse(ConcoctionCraftQueue.push("Lucky Lindy", 2, context))
        assertEquals(1, ConcoctionQueueBudget.queuedSpeakeasyDrink)
    }

    @Test
    fun reserveAffirmationCookie_incrementsCounter() {
        registerItem(ConcoctionSpecialQueue.AFFIRMATION_COOKIE, "Affirmation Cookie")

        val delta = ConcoctionSpecialQueue.reserve("Affirmation Cookie", quantity = 4, ConcoctionQueueContext())

        assertEquals(1, ConcoctionQueueBudget.queuedAffirmationCookies)
        assertEquals(1, delta.affirmationCookiesUsed)
    }

    private fun registerSpeakeasyDrink(id: Int, name: String) {
        registerItem(id, name)
        DailyLimitDatabase.registerEntryForTest(
            DailyLimitEntry(
                kind = DailyLimitKind.DRINK,
                itemId = id,
                trackingProperty = "_speakeasyDrinksDrunk",
                maxValue = 3,
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
