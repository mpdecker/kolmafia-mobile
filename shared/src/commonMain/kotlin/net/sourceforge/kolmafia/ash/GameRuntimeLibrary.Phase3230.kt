package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.YouRobotManager

/**
 * ASH helpers for YouRobot / Grimstone / Rumple mega (Phases 3171–3230).
 * Desktop exposes only my_robot_energy / my_robot_scraps; helpers deepen pref introspection.
 */
internal fun GameRuntimeLibrary.registerPhase3230(scope: AshScope) {
    // Live energy/scraps already registered in AshP11; deepen with part helpers.
    regFn(scope, "have_robot_part", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { _, args ->
        AshValue.of(YouRobotManager.hasEquipped(args[0].toString()))
    }
    regFn(scope, "robot_can_use_familiars", AshType.BOOLEAN, emptyList()) { _, _ ->
        val inRobo = character?.state?.value?.inRobocore == true
        AshValue.of(!inRobo || YouRobotManager.canUseFamiliars())
    }
    regFn(scope, "robot_can_use_potions", AshType.BOOLEAN, emptyList()) { _, _ ->
        val inRobo = character?.state?.value?.inRobocore == true
        AshValue.of(!inRobo || YouRobotManager.canUsePotions())
    }
}
