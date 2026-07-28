package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType

class ConcoctionMethodGatesTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun staff_permittedForMysticalityWithGuildOpen() {
        val prefs = prefs()
        prefs.setInt("lastGuildStoreOpen", 5)
        val state = CharacterState(
            characterClass = CharacterClass.SAUCEROR.id,
            ascensionNumber = 5,
        )
        assertTrue(
            ConcoctionMethodGates.isPermitted("STAFF", state, prefs, { 0 }),
        )
    }

    @Test
    fun staff_blockedForMuscleClass() {
        val prefs = prefs()
        prefs.setInt("lastGuildStoreOpen", 5)
        val state = CharacterState(
            characterClass = CharacterClass.SEAL_CLUBBER.id,
            ascensionNumber = 5,
        )
        assertFalse(
            ConcoctionMethodGates.isPermitted("STAFF", state, prefs, { 0 }),
        )
    }

    @Test
    fun staff_blockedWhenGuildNotOpenThisAscension() {
        val prefs = prefs()
        prefs.setInt("lastGuildStoreOpen", 4)
        val state = CharacterState(
            characterClass = CharacterClass.SAUCEROR.id,
            ascensionNumber = 5,
        )
        assertFalse(
            ConcoctionMethodGates.isPermitted("STAFF", state, prefs, { 0 }),
        )
    }

    @Test
    fun phineas_permittedWithSledgehammerAccessible() {
        assertTrue(
            ConcoctionMethodGates.isPermitted(
                "PHINEAS",
                CharacterState(),
                prefs(),
                accessibleCount = { if (it == 4316) 1 else 0 },
            ),
        )
    }

    @Test
    fun phineas_blockedWithoutSledgehammer() {
        assertFalse(
            ConcoctionMethodGates.isPermitted("PHINEAS", CharacterState(), prefs(), { 0 }),
        )
    }

    @Test
    fun cookFancy_permittedWithFreeCookingTurns() {
        val prefs = prefs()
        prefs.setBoolean("hasRange", true)
        prefs.setInt("_elfGuardCookingUsed", 0)
        val state = CharacterState(adventuresLeft = 0)
        val skills = listOf(
            SkillData(229, "Elf Guard Cooking", SkillType.PASSIVE, mpCost = 0, dailyLimit = 0, timesCast = 0),
        )
        assertTrue(
            ConcoctionMethodGates.isPermitted("COOK_FANCY", state, prefs, { 0 }, skills = skills),
        )
    }

    @Test
    fun mixFancy_permittedWithCocktailMagic() {
        val prefs = prefs()
        prefs.setBoolean("hasCocktailKit", true)
        val state = CharacterState(adventuresLeft = 0)
        val skills = listOf(
            SkillData(15008, "Cocktail Magic", SkillType.PASSIVE, mpCost = 0, dailyLimit = 0, timesCast = 0),
        )
        assertTrue(
            ConcoctionMethodGates.isPermitted("MIX_FANCY", state, prefs, { 0 }, skills = skills),
        )
    }

    @Test
    fun mixFancy_blockedByRequireBoxServantsWithoutBartender() {
        val prefs = prefs()
        prefs.setBoolean("hasCocktailKit", true)
        prefs.setBoolean("requireBoxServants", true)
        val state = CharacterState(adventuresLeft = 5)
        assertFalse(
            ConcoctionMethodGates.isPermitted("MIX_FANCY", state, prefs, { 0 }),
        )
    }

    @Test
    fun cookFancy_blockedWithoutRangeAndWithoutWillBuyTool() {
        val prefs = prefs()
        val state = CharacterState(meat = 2000, adventuresLeft = 5)
        assertFalse(
            ConcoctionMethodGates.isPermitted("COOK_FANCY", state, prefs, { 0 }),
        )
    }

    @Test
    fun cookFancy_permittedWithWillBuyToolWithoutRangeWhenAdventuresRemain() {
        val prefs = prefs()
        prefs.setBoolean("autoSatisfyWithNPCs", true)
        val state = CharacterState(meat = 2000, adventuresLeft = 3)
        assertTrue(
            ConcoctionMethodGates.isPermitted("COOK_FANCY", state, prefs, { 0 }),
        )
    }

    @Test
    fun mixFancy_permittedWithWillBuyServantWithoutBartender() {
        val prefs = prefs()
        prefs.setBoolean("hasCocktailKit", true)
        prefs.setBoolean("autoRepairBoxServants", true)
        prefs.setBoolean("autoSatisfyWithMall", true)
        val state = CharacterState(adventuresLeft = 0)
        assertTrue(
            ConcoctionMethodGates.isPermitted("MIX_FANCY", state, prefs, { 0 }),
        )
    }
}
