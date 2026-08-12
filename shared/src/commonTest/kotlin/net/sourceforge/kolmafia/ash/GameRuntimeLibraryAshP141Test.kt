package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.ConcoctionMethodGates
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType

class GameRuntimeLibraryAshP141Test {

    private fun prefs(autoNpc: Boolean = false): Preferences {
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithNPCs", autoNpc)
        return p
    }

    private val clipArtSkill = SkillData(
        7216,
        "Summon Clip Art",
        SkillType.COMBAT,
        mpCost = 0,
        dailyLimit = 3,
        timesCast = 0,
    )

    @Test
    fun revision_phase176() {
        assertEquals("phase470", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun concoctionPermitted_smithViaKnollWithoutHammer() {
        val concoction = ConcoctionData(
            result = "corpus smith knoll",
            resultQuantity = 1,
            methods = setOf("SMITH"),
            ingredients = emptyList(),
        )
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(zodiacSign = "Wallaby"),
                prefs = prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun concoctionPermitted_hammerTokenUsesSmithingGates() {
        val concoction = ConcoctionData(
            result = "corpus hammer item",
            resultQuantity = 1,
            methods = setOf("COMBINE", "HAMMER"),
            ingredients = emptyList(),
        )
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(zodiacSign = "Mongoose"),
                prefs = prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun concoctionPermitted_clipArtRequiresSkill() {
        val concoction = ConcoctionData(
            result = "corpus clip art",
            resultQuantity = 1,
            methods = setOf("CLIPART"),
            ingredients = emptyList(),
        )
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                prefs = prefs(),
                accessibleCount = { 0 },
            ),
        )
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                skills = listOf(clipArtSkill),
                prefs = prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun concoctionMethodGates_jewelryRequiresPliers() {
        assertTrue(
            ConcoctionMethodGates.isPermitted(
                "JEWEL",
                CharacterState(),
                prefs = null,
                accessibleCount = { if (it == 7709) 1 else 0 },
            ),
        )
    }
}
