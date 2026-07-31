package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility
import net.sourceforge.kolmafia.shop.FiveDPrinterAccessibility

class GameRuntimeLibraryAshP144Test {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun starchartMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "Star Chart",
            nickname = "starchart",
            token = "star chart",
            shopId = "starchart",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun sugarsheetsMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "Sugar Sheets",
            nickname = "sugarsheets",
            token = "sugar sheet",
            shopId = "sugarsheets",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun fiveDPrinterMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "Xiblaxian 5D printer",
            nickname = "5dprinter",
            token = null,
            shopId = "5dprinter",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    @Test
    fun revision_phase176() {
        assertEquals("phase270", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun starchart_starShirtRequiresTorsoAwareness() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                starchartMaster(),
                1133,
                CharacterState(level = 10),
                prefs(),
                accessibleCount = { 0 },
                hasSkill = { false },
            ),
        )
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                starchartMaster(),
                1133,
                CharacterState(level = 10),
                prefs(),
                accessibleCount = { 0 },
                hasSkill = { it == 12 },
            ),
        )
    }

    @Test
    fun sugarsheets_sugarShirtRequiresTorsoAwareness() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                sugarsheetsMaster(),
                4191,
                CharacterState(level = 10),
                prefs(),
                accessibleCount = { 0 },
                hasSkill = { false },
            ),
        )
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                sugarsheetsMaster(),
                4191,
                CharacterState(level = 10),
                prefs(),
                accessibleCount = { 0 },
                hasSkill = { it == 15022 },
            ),
        )
    }

    @Test
    fun fiveDPrinter_shopBlockedWithoutPrinter() {
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                fiveDPrinterMaster(),
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                fiveDPrinterMaster(),
                CharacterState(),
                prefs(),
                accessibleCount = { if (it == FiveDPrinterAccessibility.FIVE_D_PRINTER) 1 else 0 },
            ),
        )
    }

    @Test
    fun fiveDPrinter_unknownRecipeBlocksPurchase() {
        val p = prefs()
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                fiveDPrinterMaster(),
                7753,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
        p.setBoolean("unknownRecipe7753", false)
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                fiveDPrinterMaster(),
                7753,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
    }
}
