package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.preferences.Preferences

/** One ordered hook for the remaining fight-time IoTM state writers. */
object FightIotmResidualSync {
    fun apply(
        html: String?,
        monsterName: String,
        preferences: Preferences?,
        itemCount: (Int) -> Int = { 0 },
        daylightShavingsEquipped: Boolean = false,
        cursedMagnifyingGlassEquipped: Boolean? = null,
        ignoreSpecial: Boolean = false,
        locationName: String? = null,
        currentRun: Int = 0,
        familiarHasStillSuit: Boolean = false,
        anyOwnedFamiliarHasStillSuit: Boolean = false,
        crystalBallEquipped: Boolean = false,
    ): Boolean {
        if (html.isNullOrBlank() || preferences == null) return false
        var changed = false
        if (monsterName.contains("Dad Sea Monkee", true)) changed = DadManager.solve(html) || changed
        if (monsterName.contains("unusual construct", true)) changed = UnusualConstructManager.solve(html) || changed
        changed = CursedMagnifyingGlassManager.updatePreference(html, preferences) || changed
        if (LocketManager.isLocketFight(html)) {
            changed = LocketManager.parseFight(monsterName, preferences) || changed
        }
        changed = StillSuitManager.handleSweat(
            html, preferences, familiarHasStillSuit, anyOwnedFamiliarHasStillSuit,
        ) || changed
        if (crystalBallEquipped) {
            changed = CrystalBallManager.parseCrystalBall(
                html, locationName, currentRun, preferences,
            ) || changed
            changed = CrystalBallManager.updateCrystalBallPredictions(
                locationName, currentRun, preferences,
            ) || changed
        }
        changed = JuneCleaverManager.updatePreferences(html, preferences) || changed
        if (daylightShavingsEquipped) {
            changed = DaylightShavingsHelmetManager.updatePreference(
                html, preferences, itemCount, equipped = true,
            ) || changed
        }
        if (monsterName.startsWith("void ", true) || monsterName.equals("void monster", true)) {
            if (!ignoreSpecial) {
                val next = if (html.contains("Time seems to stop.", true)) {
                    (preferences.getInt("_voidFreeFights", 0) + 1).coerceAtMost(5)
                } else {
                    5
                }
                preferences.setInt("_voidFreeFights", next)
                if (preferences.getInt("cursedMagnifyingGlassCount", 0) == 13 &&
                    (cursedMagnifyingGlassEquipped
                        ?: (itemCount(ItemPool.CURSED_MAGNIFYING_GLASS) > 0))
                ) {
                    CursedMagnifyingGlassManager.reset(preferences)
                }
                changed = true
            }
        }
        return changed
    }
}
