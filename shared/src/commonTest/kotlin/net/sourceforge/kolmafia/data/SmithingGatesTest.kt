package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType

class SmithingGatesTest {

    private fun prefs(autoNpc: Boolean = false): Preferences {
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithNPCs", autoNpc)
        return p
    }

    @Test
    fun smithPermitted_withHammer() {
        assertTrue(
            SmithingGates.isSmithPermitted(
                CharacterState(),
                prefs(),
                accessibleCount = { if (it == SmithingGates.TENDERIZING_HAMMER) 1 else 0 },
            ),
        )
    }

    @Test
    fun smithPermitted_withWillBuyTool() {
        assertTrue(
            SmithingGates.isSmithPermitted(
                CharacterState(meat = 1500),
                prefs(autoNpc = true),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun smithPermitted_withKnollSignWithoutHammer() {
        assertTrue(
            SmithingGates.isSmithPermitted(
                CharacterState(zodiacSign = "Mongoose"),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun smithBlocked_inZombiecoreEvenWithKnoll() {
        assertFalse(
            SmithingGates.isSmithPermitted(
                CharacterState(
                    zodiacSign = "Mongoose",
                    challengePath = AscensionPath.ZOMBIE_SLAYER.apiName,
                ),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun smithBlocked_withoutHammerToolOrKnoll() {
        assertFalse(
            SmithingGates.isSmithPermitted(
                CharacterState(zodiacSign = "Wombat"),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun knollAvailability_matchesDesktopSigns() {
        assertTrue(KnollAvailability.isAvailable(CharacterState(zodiacSign = "Vole")))
        assertFalse(KnollAvailability.isAvailable(CharacterState(zodiacSign = "Wombat")))
        assertFalse(
            KnollAvailability.isAvailable(
                CharacterState(
                    zodiacSign = "Mongoose",
                    challengePath = AscensionPath.KINGDOM_OF_EXPLOATHING.apiName,
                ),
            ),
        )
    }
}

class ClipArtGatesTest {

    private fun prefs(summons: Int = 0): Preferences {
        val p = Preferences(MapSettings())
        p.setInt("_clipartSummons", summons)
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
    fun clipArtPermitted_withSkillAndSummonsRemaining() {
        assertTrue(
            ConcoctionMethodGates.isPermitted(
                "CLIPART",
                CharacterState(),
                prefs(summons = 1),
                accessibleCount = { 0 },
                skills = listOf(clipArtSkill),
            ),
        )
    }

    @Test
    fun clipArtBlocked_withoutSkill() {
        assertFalse(
            ConcoctionMethodGates.isPermitted(
                "CLIPART",
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
                skills = emptyList(),
            ),
        )
    }

    @Test
    fun clipArtBlocked_inBadMoon() {
        assertFalse(
            ConcoctionMethodGates.isPermitted(
                "CLIPART",
                CharacterState(zodiacSign = "Bad Moon"),
                prefs(),
                accessibleCount = { 0 },
                skills = listOf(clipArtSkill),
            ),
        )
    }

    @Test
    fun clipArtBlocked_whenSummonsExhausted() {
        assertFalse(
            ConcoctionMethodGates.isPermitted(
                "CLIPART",
                CharacterState(),
                prefs(summons = 3),
                accessibleCount = { 0 },
                skills = listOf(clipArtSkill),
            ),
        )
    }

    @Test
    fun jewelryPermitted_withPliers() {
        assertTrue(
            ConcoctionMethodGates.isPermitted(
                "JEWELRY",
                CharacterState(),
                prefs = null,
                accessibleCount = { if (it == 709) 1 else 0 },
            ),
        )
    }
}
