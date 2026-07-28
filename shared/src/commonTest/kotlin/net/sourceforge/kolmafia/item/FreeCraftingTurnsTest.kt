package net.sourceforge.kolmafia.item

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType

class FreeCraftingTurnsTest {

    @Test
    fun freeCraftingTurns_inigoEffectDuration() {
        val context = FreeCraftingTurns.Context(
            effects = listOf(EffectData(id = 716, name = "Inigo's Incantation of Inspiration", duration = 10)),
        )
        assertEquals(2, FreeCraftingTurns.freeCraftingTurns(context))
    }

    @Test
    fun freeCraftingTurns_rapidPrototypingSkill() {
        val prefs = Preferences(MapSettings())
        val context = FreeCraftingTurns.Context(
            preferences = prefs,
            skills = listOf(SkillData(id = 125, name = "Rapid Prototyping", type = SkillType.PASSIVE, mpCost = 0, dailyLimit = 0, timesCast = 0)),
        )
        assertEquals(5, FreeCraftingTurns.freeCraftingTurns(context))
    }

    @Test
    fun freeSmithingTurns_warbearAutoAnvil() {
        val prefs = Preferences(MapSettings())
        val context = FreeCraftingTurns.Context(
            preferences = prefs,
            itemCount = { if (it == 6965) 1 else 0 },
        )
        assertEquals(5, FreeCraftingTurns.freeSmithingTurns(context))
    }

    @Test
    fun freeCookingTurns_cookbookbatFamiliar() {
        val prefs = Preferences(MapSettings())
        val context = FreeCraftingTurns.Context(
            preferences = prefs,
            ownedFamiliar = { it.equals("Cookbookbat", ignoreCase = true) },
        )
        assertEquals(5, FreeCraftingTurns.freeCookingTurns(context))
    }

    @Test
    fun freeCocktailcraftingTurns_oldSchoolSkill() {
        val prefs = Preferences(MapSettings())
        val context = FreeCraftingTurns.Context(
            preferences = prefs,
            skills = listOf(SkillData(id = 230, name = "Old-School Cocktailcrafting", type = SkillType.PASSIVE, mpCost = 0, dailyLimit = 0, timesCast = 0)),
        )
        assertEquals(3, FreeCraftingTurns.freeCocktailcraftingTurns(context))
    }

    @Test
    fun freeCraftingTurns_expertCornerCutterNeedsAdventuresLeft() {
        val prefs = Preferences(MapSettings())
        val context = FreeCraftingTurns.Context(
            preferences = prefs,
            state = CharacterState(adventuresLeft = 0),
            skills = listOf(SkillData(id = 177, name = "Expert Corner-Cutter", type = SkillType.PASSIVE, mpCost = 0, dailyLimit = 0, timesCast = 0)),
        )
        assertEquals(0, FreeCraftingTurns.freeCraftingTurns(context))
    }
}
