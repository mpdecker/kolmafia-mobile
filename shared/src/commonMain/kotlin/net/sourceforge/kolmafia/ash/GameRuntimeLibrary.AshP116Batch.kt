package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.concoction.StillsAvailability

/**
 * ASH-P116 behavioral batch — stills count and mushroom plot ownership.
 */
internal fun GameRuntimeLibrary.registerAshP116Batch(scope: AshScope) {
    regFn(scope, "stills_available", AshType.INT, emptyList()) { _, _ ->
        val state = character?.state?.value ?: return@regFn AshValue.of(0L)
        val asc = state.ascensionNumber
        val guildOpen = preferences?.getInt("lastGuildStoreOpen", -1) == asc
        val hasSkill = { id: Int ->
            skillManager?.state?.value?.skills?.any { it.id == id } == true
        }
        AshValue.of(
            StillsAvailability.gatedCount(state, hasSkill, guildOpen).toLong(),
        )
    }

    regFn(scope, "have_mushroom_plot", AshType.BOOLEAN, emptyList()) { _, _ ->
        val asc = character?.state?.value?.ascensionNumber ?: 0
        val lastPlot = preferences?.getInt("lastMushroomPlot", -1) ?: -1
        AshValue.of(asc == lastPlot)
    }
}
