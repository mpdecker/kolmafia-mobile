package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.BadMoonManager
import net.sourceforge.kolmafia.session.GreyYouManager
import net.sourceforge.kolmafia.request.SpaaaceRequest

/** ASH helpers for Grey You / Valhalla / Bad Moon / Spaaace mega (Phases 3291–3350). */
internal fun GameRuntimeLibrary.registerPhase3350(scope: AshScope) {
    regFn(scope, "grey_you_goo_skill_count", AshType.INT, emptyList()) { _, _ ->
        GreyYouManager.loadRegistry()
        AshValue.of(GreyYouManager.allGooSkills.size)
    }
    regFn(scope, "grey_you_zone_count", AshType.INT, emptyList()) { _, _ ->
        GreyYouManager.loadRegistry()
        AshValue.of(GreyYouManager.zoneAbsorptions.size)
    }
    regFn(scope, "grey_you_adventures", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(preferences?.getInt("_greyYouAdventures", 0) ?: 0)
    }
    regFn(scope, "bad_moon_encounters_done", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(BadMoonManager.completedCount(preferences))
    }
    regFn(scope, "porko_best_expected", AshType.FLOAT, emptyList()) { _, _ ->
        AshValue.of(SpaaaceRequest.bestExpectedPayout()?.toDouble() ?: 0.0)
    }
    regFn(scope, "in_valhalla", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(net.sourceforge.kolmafia.character.CharpaneValhallaSync.inValhalla)
    }
    regFn(scope, "banked_karma", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(preferences?.getInt("bankedKarma", 0) ?: 0)
    }
}
