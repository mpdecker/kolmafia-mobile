package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop KOLHS after-school coinmaster [accessible] gates
 * (Art / Chem / Shop Class).
 *
 * Desktop: [KoLCharacter.inHighschool] (Path.KOLHS) &&
 * `lastKOLHS*ClassUnlockAdventure == getCurrentRun()`.
 * Mobile uses [CharacterState.inKoLHS] / path + [CharacterState.currentRun]
 * (soft parity if highschool class-period state is not modeled beyond unlock pref).
 */
object KolhsShopAccessibility {

    fun inaccessibleReason(
        char: CharacterState,
        prefs: Preferences?,
        unlockPref: String,
        classLabel: String,
    ): String? {
        val inHighschool =
            char.inKoLHS ||
                char.ascensionPath == AscensionPath.KOLHS ||
                char.challengePath.equals(AscensionPath.KOLHS.apiName, ignoreCase = true)
        // Desktop compares unlock pref to getCurrentRun() (turns this ascension).
        val unlockedThisAdventure =
            prefs?.getInt(unlockPref, -1) == char.currentRun
        return if (inHighschool && unlockedThisAdventure) {
            null
        } else {
            "You need to be in $classLabel to make that."
        }
    }
}
