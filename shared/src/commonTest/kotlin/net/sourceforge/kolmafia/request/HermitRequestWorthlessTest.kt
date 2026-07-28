package net.sourceforge.kolmafia.request

import kotlin.test.Test
import kotlin.test.assertEquals

class HermitRequestWorthlessTest {

    @Test
    fun worthlessCountFromMaps_sumsTrinketGewgawAndKnickKnack() {
        val count = HermitRequest.worthlessCountFromMaps(
            inventory = mapOf(
                HermitRequest.WORTHLESS_TRINKET_ID to 2,
                HermitRequest.WORTHLESS_GEWGAW_ID to 1,
            ),
            closet = mapOf(HermitRequest.WORTHLESS_KNICK_KNACK_ID to 3),
            storage = mapOf(HermitRequest.WORTHLESS_TRINKET_ID to 1),
        )
        assertEquals(7, count)
    }
}
