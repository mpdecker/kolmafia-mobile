package net.sourceforge.kolmafia.item

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionRefreshContext
import net.sourceforge.kolmafia.data.FloundryAvailability
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings

class CreatableAmountTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        HotDogAvailability.resetForTest()
        SpeakeasyAvailability.resetForTest()
        FloundryAvailability.resetForTest()
    }

    @Test
    fun quantityPossible_limitedByScarcestIngredient() {
        registerItem(3001, "result item")
        registerItem(3002, "ingredient a")
        registerItem(3003, "ingredient b")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "result item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("ingredient a", 2),
                    ConcoctionIngredient("ingredient b", 3),
                ),
            ),
        )
        val counts = mapOf(
            3002 to 10,
            3003 to 9,
        )
        val possible = CreatableAmount.quantityPossible(
            3001,
            accessibleCount = { id, _ -> counts[id] ?: 0 },
            preferRuntime = false,
        )
        assertEquals(3, possible)
    }

    @Test
    fun quantityPossible_respectsResultQuantity() {
        registerItem(3101, "bulk result")
        registerItem(3102, "single ingredient")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "bulk result",
                resultQuantity = 3,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("single ingredient", 1)),
            ),
        )
        val possible = CreatableAmount.quantityPossible(
            3101,
            accessibleCount = { _, _ -> 4 },
            preferRuntime = false,
        )
        assertEquals(12, possible)
    }

    @Test
    fun quantityPossible_zeroWhenNoConcoction() {
        registerItem(3201, "not craftable")
        assertEquals(0, CreatableAmount.quantityPossible(3201, accessibleCount = { _, _ -> 99 }, preferRuntime = false))
    }

    @Test
    fun quantityPossible_prefersRuntimeAfterRefresh_nestedParent() {
        registerItem(8001, "runtime parent")
        registerItem(8002, "runtime leaf")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "runtime leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "runtime parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("runtime leaf", 1)),
            ),
        )

        val flatOnly = CreatableAmount.quantityPossible(
            8001,
            accessibleCount = { _, _ -> 0 },
            preferRuntime = false,
        )
        assertEquals(0, flatOnly)

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromAggregatedCounts(mapOf(8002 to 2)),
        )
        assertEquals(2, CreatableAmount.quantityPossible(8001, accessibleCount = { _, _ -> 0 }))
    }

    @Test
    fun quantityPossible_adventureLimitedRefresh_returnsZeroCreatable() {
        registerItem(8101, "adv amount parent")
        registerItem(8102, "adv amount leaf")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "adv amount leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "adv amount parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("adv amount leaf", 1)),
            ),
        )

        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromLiveSession(
                aggregatedCounts = mapOf(8102 to 5),
                state = net.sourceforge.kolmafia.character.CharacterState(adventuresLeft = 0),
                accessibleCount = { id -> if (id == 8102) 5 else 0 },
            ),
        )

        assertEquals(0, CreatableAmount.quantityPossible(8101, accessibleCount = { _, _ -> 0 }))
    }

    @Test
    fun quantityPossible_hotDogUsesTotalCountNotCreatable() {
        registerItem(9201, "basic hot dog")
        HotDogAvailability.addForTest("basic hot dog")
        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext(
                characterState = CharacterState(),
                preferences = Preferences(MapSettings()),
            ),
        )
        assertEquals(1, CreatableAmount.quantityPossible(9201, accessibleCount = { _, _ -> 0 }))
    }

    @Test
    fun quantityPossible_speakeasyUsesTotalCountNotCreatable() {
        registerItem(7592, "Lucky Lindy")
        SpeakeasyAvailability.addLoungeId(4)
        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext(
                characterState = CharacterState(meat = 500),
                preferences = Preferences(MapSettings()),
            ),
        )
        assertEquals(1, CreatableAmount.quantityPossible(7592, accessibleCount = { _, _ -> 0 }))
    }

    @Test
    fun quantityPossible_floundryUsesTotalCountNotCreatable() {
        registerItem(9001, "carpe")
        FloundryAvailability.addForTest("carpe", 250)
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true)
        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext(
                characterState = CharacterState(),
                preferences = prefs,
            ),
        )
        assertEquals(25, CreatableAmount.quantityPossible(9001, accessibleCount = { _, _ -> 0 }))
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
