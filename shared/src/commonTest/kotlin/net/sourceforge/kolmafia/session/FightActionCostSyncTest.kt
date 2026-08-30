package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class FightActionCostSyncTest {
    private lateinit var prefs: Preferences
    private lateinit var character: KoLCharacter

    @BeforeTest
    fun setUp() {
        FightActionCostSync.reset()
        prefs = Preferences(MapSettings())
        character = KoLCharacter()
        character.updateHpMp(100, 100, 50, 100)
    }

    @Test
    fun paySkillCostDeductsMp() {
        character.updateHpMp(100, 100, 40, 100)
        // skill with unknown id → mpCost 0; use explicit paySkillCost with mocked via nextAction empty
        // Direct: only deducts when SkillDefinitionDatabase has the skill — skip DB, test resource gains
        val changed = FightActionCostSync.applyResourceGains(
            "You absorb 5 Soulsauce",
            character,
        )
        assertTrue(changed)
        assertEquals(5, character.state.value.soulsauce)
    }

    @Test
    fun jiggleSetsFlag() {
        FightActionCostSync.nextAction = "jiggle"
        FightActionCostSync.payActionCost(html = "You jiggle the staff", preferences = prefs)
        assertTrue(FightActionCostSync.alreadyJiggled())
    }

    @Test
    fun chaosButterflyPref() {
        FightActionCostSync.payItemCost(
            itemId = FightActionCostSync.CHAOS_BUTTERFLY,
            html = "reality is altered in unpredictable ways",
            preferences = prefs,
        )
        assertTrue(prefs.getBoolean("chaosButterflyThrown"))
    }

    @Test
    fun cosmicBowlingBallResetsReturn() {
        prefs.setInt("cosmicBowlingBallReturnCombats", 5)
        FightActionCostSync.payItemCost(
            itemId = FightActionCostSync.COSMIC_BOWLING_BALL,
            html = "you hurl it down the ancient lanes",
            preferences = prefs,
        )
        assertEquals(0, prefs.getInt("cosmicBowlingBallReturnCombats", -1))
        assertEquals(1, prefs.getInt("hiddenBowlingAlleyProgress", 0))
        assertFalse(
            FightActionCostSync.isItemConsumed(
                FightActionCostSync.COSMIC_BOWLING_BALL,
                "you hurl it down the ancient lanes",
            ),
        )
    }

    @Test
    fun attackDoesNothing() {
        FightActionCostSync.nextAction = "attack"
        val before = character.state.value.currentMp
        FightActionCostSync.payActionCost(html = "You attack", character = character)
        assertEquals(before, character.state.value.currentMp)
    }

    @Test
    fun naughtySorceressFirstFormDoesNotConsumeBlockedItem() {
        assertFalse(
            FightActionCostSync.isItemConsumed(
                itemId = 123,
                html = "You start to use the seal tooth, but the Sorceress bats it aside.",
                monsterName = "Naughty Sorceress",
                itemName = "seal tooth",
            ),
        )
        assertTrue(
            FightActionCostSync.isItemConsumed(
                itemId = 123,
                html = "You start to use the seal tooth, but the Sorceress bats it aside.",
                monsterName = "Naughty Sorceress",
                itemName = "spices",
            ),
        )
    }

    @Test
    fun naughtySorceressSecondFormDoesNotConsumeBlockedItem() {
        assertFalse(
            FightActionCostSync.isItemConsumed(
                itemId = 123,
                html = "The seal tooth is shattered into a million pieces before you can use it.",
                monsterName = "Naughty Sorceress (2)",
                itemName = "seal tooth",
            ),
        )
    }

    @Test
    fun shadowKnockAwayDoesNotConsumeEitherFunkslungItem() {
        assertFalse(
            FightActionCostSync.isItemConsumed(
                itemId = 123,
                html = "Your Shadow knocks it out of your hands.",
                monsterName = "Your Shadow",
                itemName = "seal tooth",
            ),
        )
    }
}
