package net.sourceforge.kolmafia.mall

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class MallSearchDecoratorTest {

    @AfterTest
    fun tearDown() {
        MallPurchaseRequest.resetStoreFilters()
    }

    @Test
    fun decorateAddBuyButtons_replacesNobuyersCell() {
        val html = """
            <tr class="graybelow"><td class="stock">3</td>
            whichstore=102069&searchitem=799&searchprice=100"><b>Store</b>
            <td valign="center" class="buyers">&nbsp;</td></tr>
        """.trimIndent()
        val decorated = MallSearchDecorator.decorateMallSearch(html, passwordHash = "abc")
        assertTrue(decorated.contains("class=\"buyone\">buy</a>"))
        assertTrue(decorated.contains("whichstore=102069"))
        assertTrue(decorated.contains("pwd=abc"))
    }

    @Test
    fun decorateHighlightStores_marksForbiddenRows() {
        MallPurchaseRequest.addForbiddenStore(102069)
        val html = """
            <tr class="graybelow"><td class="stock">3</td>
            whichstore=102069&searchitem=799&searchprice=100"><b>Store</b>
            <td valign="center" class="buyers">&nbsp;</td></tr>
        """.trimIndent()
        val decorated = MallSearchDecorator.decorateMallSearch(html)
        assertTrue(decorated.contains("graybelow forbidden"))
    }
}
