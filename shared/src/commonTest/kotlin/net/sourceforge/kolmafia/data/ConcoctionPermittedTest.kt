package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.Gender
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings

class ConcoctionPermittedTest {

    @AfterTest
    fun cleanup() {
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun isPermittedMethod_sspdRecipe_blockedOffHoliday() {
        val concoction = ConcoctionData(
            result = "pete smoothie",
            resultQuantity = 1,
            methods = setOf("COOK", "SSPD"),
            ingredients = emptyList(),
        )
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                kolHoliday = "",
            ),
        )
    }

    @Test
    fun isPermittedMethod_sspdRecipe_allowedOnHoliday() {
        val concoction = ConcoctionData(
            result = "pete smoothie",
            resultQuantity = 1,
            methods = setOf("COOK", "SSPD"),
            ingredients = emptyList(),
        )
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                kolHoliday = "St. Sneaky Pete's Day",
            ),
        )
    }

    @Test
    fun isPermittedMethod_grimaciteRecipe_requiresHammer() {
        val concoction = ConcoctionData(
            result = "grim widget",
            resultQuantity = 1,
            methods = setOf("SMITH", "GRIMACITE"),
            ingredients = emptyList(),
        )
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                accessibleCount = { 0 },
            ),
        )
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                accessibleCount = { id -> if (id == 3542 || id == 338) 1 else 0 },
            ),
        )
    }

    @Test
    fun isPermittedMethod_femaleRecipe_blockedForMale() {
        val concoction = ConcoctionData(
            result = "pink brew",
            resultQuantity = 1,
            methods = setOf("COMBINE", "FEMALE"),
            ingredients = emptyList(),
        )
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(gender = Gender.MALE),
            ),
        )
    }

    @Test
    fun isPermittedMethod_still_requiresStillsAvailable() {
        val concoction = ConcoctionData(
            result = "still brew",
            resultQuantity = 1,
            methods = setOf("STILL"),
            ingredients = emptyList(),
        )
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(stillsAvailable = 0),
            ),
        )
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(stillsAvailable = 2),
            ),
        )
    }

    @Test
    fun isPermittedMethod_coinmaster_requiresPrefAndProbe() {
        registerItem(9601, "coin result")
        registerItem(9602, "coin token")
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.loadFromText(
            shopsText = "shore\tThe Shore, Inc. Gift Shop\n",
            coinText = "The Shore, Inc. Gift Shop\tROW9601\tcoin result (1)\tcoin token (5)\n",
        )
        val concoction = ConcoctionData(
            result = "coin result",
            resultQuantity = 1,
            methods = setOf("COINMASTER"),
            ingredients = emptyList(),
        )
        val prefs = Preferences(MapSettings())
        val state = CharacterState(level = 10, kingLiberated = true)
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = prefs,
                accessibleCount = { 99 },
            ),
        )
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = prefs,
                accessibleCount = { id -> if (id == 9602) 10 else 0 },
            ),
        )
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
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
