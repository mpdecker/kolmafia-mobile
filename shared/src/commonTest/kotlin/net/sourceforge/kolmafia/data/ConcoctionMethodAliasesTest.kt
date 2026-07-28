package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConcoctionMethodAliasesTest {

    @Test
    fun normalize_tinkerMapsToGnomeTinker() {
        val normalized = ConcoctionMethodAliases.normalize(setOf("TINKER"))
        assertTrue("GNOME_TINKER" in normalized)
        assertEquals(false, "TINKER" in normalized)
    }

    @Test
    fun normalize_wsmithMapsToSSmithAndWeapon() {
        val normalized = ConcoctionMethodAliases.normalize(setOf("WSMITH"))
        assertTrue("SSMITH" in normalized)
        assertTrue("WEAPON" in normalized)
    }

    @Test
    fun normalize_sauceMapsToCookFancyAndReagent() {
        val normalized = ConcoctionMethodAliases.normalize(setOf("SAUCE"))
        assertTrue("COOK_FANCY" in normalized)
        assertTrue("REAGENT" in normalized)
    }

    @Test
    fun normalize_acockMapsToMixFancyAndAc() {
        val normalized = ConcoctionMethodAliases.normalize(setOf("ACOCK"))
        assertTrue("MIX_FANCY" in normalized)
        assertTrue("AC" in normalized)
    }

    @Test
    fun primaryMethod_wsmithResolvesToSSmith() {
        assertEquals(
            "SSMITH",
            ConcoctionCreationCost.primaryMethod(setOf("WSMITH")),
        )
    }
}
