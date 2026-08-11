package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionCreationCost
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionMethodGates
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType

class GameRuntimeLibraryAshP145Test {

    @Test
    fun revision_phase176() {
        assertEquals("phase421", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun primaryMethod_tinkerAliasResolvesToGnomeTinker() {
        assertEquals(
            "GNOME_TINKER",
            ConcoctionCreationCost.primaryMethod(setOf("TINKER")),
        )
    }

    @Test
    fun gnomadsAvailable_requiresDesertBeach() {
        val prefs = Preferences(MapSettings())
        val state = CharacterState(zodiacSign = "Wombat", ascensionNumber = 1)
        assertFalse(
            ConcoctionMethodGates.isPermitted(
                "GNOME_TINKER",
                state,
                prefs,
                accessibleCount = { 0 },
            ),
        )
        prefs.setInt("lastDesertUnlock", 1)
        assertTrue(
            ConcoctionMethodGates.isPermitted(
                "GNOME_TINKER",
                state,
                prefs,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun clipArt_badMoonAllowedWhenSkillsRecalled() {
        val clipArt = SkillData(
            7216, "Clip Art", SkillType.COMBAT,
            mpCost = 0, dailyLimit = 0, timesCast = 0,
        )
        val prefs = Preferences(MapSettings())
        val badMoon = CharacterState(zodiacSign = "Bad Moon", skillsRecalled = true)
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                ConcoctionData(
                    result = "corpus clip art",
                    resultQuantity = 1,
                    methods = setOf("CLIPART"),
                    ingredients = emptyList(),
                ),
                badMoon,
                skills = listOf(clipArt),
                prefs = prefs,
            ),
        )
    }
}
