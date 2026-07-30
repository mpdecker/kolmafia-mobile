package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings

class SkillDefinitionDatabaseSkillProxyTest {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun getSkillTypeName_combatSpell() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 15,
                name = "CLEESH",
                image = "commacha.gif",
                tags = setOf("combat", "spell"),
                mpCost = 10,
                duration = 0,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
        assertEquals("combat", SkillDefinitionProxy.getSkillTypeName(15))
    }

    @Test
    fun getSkillTypeName_passive() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 1,
                name = "Liver of Steel",
                image = "liver.gif",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
                isPermable = false,
            ),
        )
        assertEquals("passive", SkillDefinitionProxy.getSkillTypeName(1))
    }

    @Test
    fun getPurchaseCost_byLevel() {
        assertEquals(750, SkillDefinitionProxy.getPurchaseCost(1003, 4))
        assertEquals(0, SkillDefinitionProxy.getPurchaseCost(7219, 1))
    }

    @Test
    fun getSkillLevel_unknownSkillReturnsNegativeOne() {
        assertEquals(-1, SkillDefinitionProxy.getSkillLevel(999999, prefs()))
    }

    @Test
    fun getSkillLevel_readsPref() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 1003,
                name = "Thrust-Smack",
                image = "club.gif",
                tags = setOf("combat"),
                mpCost = 3,
                duration = 0,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
        val p = prefs()
        p.setInt("skillLevel1003", 4)
        assertEquals(4, SkillDefinitionProxy.getSkillLevel(1003, p))
    }

    @Test
    fun tagHelpers() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 15,
                name = "CLEESH",
                image = "commacha.gif",
                tags = setOf("combat", "spell"),
                mpCost = 10,
                duration = 0,
                isPassive = false,
                isCombat = true,
                isNonCombat = false,
                isSong = false,
            ),
        )
        assertTrue(SkillDefinitionProxy.isCombat(15))
        assertTrue(SkillDefinitionProxy.isSpell(15))
        assertFalse(SkillDefinitionProxy.isPassive(15))
        assertTrue(SkillDefinitionProxy.isLibram(7219))
    }

    @Test
    fun isPermable_respectsDefinitionAndDefault() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 1,
                name = "Liver of Steel",
                image = "liver.gif",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
                isPermable = false,
            ),
        )
        assertFalse(SkillDefinitionProxy.isPermable(1))
        assertFalse(SkillDefinitionProxy.isPermable(9999))
    }
}
