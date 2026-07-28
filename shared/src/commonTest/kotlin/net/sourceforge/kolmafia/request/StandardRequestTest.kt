package net.sourceforge.kolmafia.request

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.modifiers.StringModifier

class StandardRequestTest {

    @AfterTest
    fun tearDown() {
        StandardRequest.resetForTest()
        TrendyRequest.resetForTest()
        ThriftyRequest.resetForTest()
        ModifierDatabase.resetForTest()
    }

    @Test
    fun parseResponse_blocksRestrictedItems() {
        StandardRequest.parseResponse(
            """
            <b>Items</b><p><span class="i">spring break beach adventure,</span><span class="i">untimely portrait</span><p>
            """.trimIndent(),
        )
        val restricted = CharacterState(isHardcore = true, roninLeft = 0)
        assertFalse(
            StandardRequest.isAllowedInStandard(
                RestrictedItemType.ITEMS,
                "spring break beach adventure",
                restricted,
            ),
        )
        assertTrue(
            StandardRequest.isAllowedInStandard(
                RestrictedItemType.ITEMS,
                "totally fine item",
                restricted,
            ),
        )
    }

    @Test
    fun isAllowed_trendyPath_blocksNonTrendyItem() {
        TrendyRequest.parseResponse(
            """
            <tr class="expired">
            <td>2004-12</td><td>Items</td><td>expired widget</td></tr>
            """.trimIndent(),
        )
        val trendy = CharacterState(challengePath = "Trendy", kingLiberated = false)
        assertFalse(
            StandardRequest.isAllowed(RestrictedItemType.ITEMS, "expired widget", trendy),
        )
        assertTrue(
            StandardRequest.isAllowed(RestrictedItemType.ITEMS, "fine widget", trendy),
        )
    }

    @Test
    fun isAllowed_thriftyPath_blocksItemWithLastAvailableDate() {
        ModifierDatabase.injectForTest(
            "Item",
            "seasonal snack",
            """Last Available: "2020-01"""",
        )
        val thrifty = CharacterState(challengePath = "Thrifty")
        assertFalse(
            StandardRequest.isAllowed(RestrictedItemType.ITEMS, "seasonal snack", thrifty),
        )
    }

    @Test
    fun isAllowed_thriftyPath_allowsEvergreenItem() {
        ModifierDatabase.injectForTest("Item", "evergreen snack", "Muscle: +1")
        val thrifty = CharacterState(challengePath = "Thrifty")
        assertTrue(
            StandardRequest.isAllowed(RestrictedItemType.ITEMS, "evergreen snack", thrifty),
        )
    }

    @Test
    fun isAllowed_quantumPath_allowsRestrictedFamiliar() {
        StandardRequest.parseResponse(
            """<b>Familiars</b><p><span class="i">banned familiar,</span><span class="i">other</span><p>""",
        )
        val quantum = CharacterState(
            challengePath = "Quantum Terrarium",
            isHardcore = true,
            roninLeft = 0,
        )
        assertTrue(
            StandardRequest.isAllowed(RestrictedItemType.FAMILIARS, "banned familiar", quantum),
        )
    }
}
