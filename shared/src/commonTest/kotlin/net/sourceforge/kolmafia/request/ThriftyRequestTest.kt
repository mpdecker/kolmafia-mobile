package net.sourceforge.kolmafia.request

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.RestrictedItemType

class ThriftyRequestTest {

    @AfterTest
    fun tearDown() {
        ThriftyRequest.resetForTest()
    }

    @Test
    fun parseResponse_allowsListedItems() {
        ThriftyRequest.parseResponse(
            """
            <b>Items</b><p><span class="i">magic mushroom,</span><span class="i">saline solution</span><p>
            <b>Skills</b><p><span class="i">absorb tentacles</span><p>
            """.trimIndent(),
        )
        assertTrue(ThriftyRequest.isAllowed(RestrictedItemType.ITEMS, "magic mushroom"))
        assertTrue(ThriftyRequest.isAllowed(RestrictedItemType.SKILLS, "Absorb Tentacles"))
        assertFalse(ThriftyRequest.isAllowed(RestrictedItemType.ITEMS, "unlisted sword"))
    }
}
