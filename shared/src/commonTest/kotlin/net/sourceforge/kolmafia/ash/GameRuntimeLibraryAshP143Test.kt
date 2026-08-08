package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionMethodGates
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.TorsoAwareness
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType

class GameRuntimeLibraryAshP143Test {

    private val tikiSkill = SkillData(
        186,
        "Tiki Mixology",
        SkillType.PASSIVE,
        mpCost = 0,
        dailyLimit = 0,
        timesCast = 0,
    )

    private val bestDressedSkill = SkillData(
        15022,
        "Best Dressed",
        SkillType.PASSIVE,
        mpCost = 0,
        dailyLimit = 0,
        timesCast = 0,
    )

    @Test
    fun revision_phase176() {
        assertEquals("phase333", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun concoctionPermitted_tikiRequiresSkill186() {
        val concoction = ConcoctionData(
            result = "corpus tiki drink",
            resultQuantity = 1,
            methods = setOf("ROLLING_PIN", "TIKI"),
            ingredients = emptyList(),
        )
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                skills = emptyList(),
            ),
        )
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                skills = listOf(tikiSkill),
            ),
        )
    }

    @Test
    fun concoctionPermitted_torsoViaBestDressed() {
        val concoction = ConcoctionData(
            result = "corpus torso shirt",
            resultQuantity = 1,
            methods = setOf("ROLLING_PIN", "TORSO"),
            ingredients = emptyList(),
        )
        assertFalse(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                skills = emptyList(),
            ),
        )
        assertTrue(
            ConcoctionPermitted.isPermittedMethod(
                concoction,
                CharacterState(),
                skills = listOf(bestDressedSkill),
            ),
        )
    }

    @Test
    fun concoctionMethodGates_rollingPinAndSewerAlwaysPermitted() {
        val state = CharacterState()
        val prefs = Preferences(MapSettings())
        assertTrue(
            ConcoctionMethodGates.isPermitted("ROLLING_PIN", state, prefs, accessibleCount = { 0 }),
        )
        assertTrue(
            ConcoctionMethodGates.isPermitted("SEWER", state, prefs, accessibleCount = { 0 }),
        )
        assertTrue(
            ConcoctionMethodGates.isPermitted("MUSE", state, prefs, accessibleCount = { 0 }),
        )
        assertTrue(
            ConcoctionMethodGates.isPermitted("SUSE", state, prefs, accessibleCount = { 0 }),
        )
    }

    @Test
    fun torsoAwareness_hasSkillCallback() {
        assertTrue(TorsoAwareness.hasTorsoAwareness { it == 15022 })
        assertFalse(TorsoAwareness.hasTorsoAwareness { false })
    }
}
