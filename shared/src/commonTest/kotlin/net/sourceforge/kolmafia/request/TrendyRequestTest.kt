package net.sourceforge.kolmafia.request

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.RestrictedItemType

class TrendyRequestTest {

    @AfterTest
    fun tearDown() {
        TrendyRequest.resetForTest()
    }

    @Test
    fun parseResponse_marksExpiredItemsNotTrendy() {
        TrendyRequest.parseResponse(
            """
            <table>
            <tr class="expired">
            <td nowrap valign="top">2004-12</td>
            <td valign="top">Items</td>
            <td valign="top">crimbo pressie, wrapping paper</td></tr>
            <tr class="">
            <td nowrap valign="top">2011-12</td>
            <td valign="top">Items</td>
            <td valign="top">fax machine</td></tr>
            </table>
            """.trimIndent(),
        )
        assertFalse(TrendyRequest.isTrendy(RestrictedItemType.ITEMS, "crimbo pressie"))
        assertTrue(TrendyRequest.isTrendy(RestrictedItemType.ITEMS, "fax machine"))
        assertTrue(TrendyRequest.isTrendy(RestrictedItemType.ITEMS, "unknown item"))
    }
}
