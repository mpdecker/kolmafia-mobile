package net.sourceforge.kolmafia.campground

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState

class CampgroundAvailabilityTest {

    @Test
    fun haveCampground_trueForStandardPath() {
        val state = CharacterState(challengePath = AscensionPath.STANDARD.apiName)
        assertTrue(CampgroundAvailability.haveCampground(state))
    }

    @Test
    fun haveCampground_falseWhenLimitModeBlocksCampground() {
        val state = CharacterState(
            challengePath = AscensionPath.STANDARD.apiName,
            limitMode = "spelunky",
        )
        assertFalse(CampgroundAvailability.haveCampground(state))
    }

    @Test
    fun haveCampground_falseForNoCampgroundPaths() {
        val blocked = listOf(
            AscensionPath.ACTUALLY_ED_THE_UNDYING,
            AscensionPath.YOU_ROBOT,
            AscensionPath.NUCLEAR_AUTUMN,
            AscensionPath.SMALL,
            AscensionPath.WEREPROFESSOR,
            AscensionPath.MEAT,
        ).map { CharacterState(challengePath = it.apiName) }
        blocked.forEach { state ->
            assertFalse(CampgroundAvailability.haveCampground(state), state.ascensionPath.name)
        }
    }
}
