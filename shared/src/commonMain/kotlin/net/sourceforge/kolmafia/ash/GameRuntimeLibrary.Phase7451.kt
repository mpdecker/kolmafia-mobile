package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.ValhallaManager

/**
 * Phases 7451–7510 — Ascension mechanics residual (Valhalla/gash/afterlife).
 * Parent wrap bumps REVISION to phase7510.
 */
internal fun GameRuntimeLibrary.registerPhase7451(scope: AshScope) {
    // Behavioral patches live in ValhallaManager / AfterLifeRequest /
    // ChoiceCombatAshState / QuestDatabase / BanishManager / BugbearManager / TurnCounter.
}

internal fun GameRuntimeLibrary.ascensionDepsFromLive(): ValhallaManager.AscensionDeps =
    ValhallaManager.AscensionDeps(
        preferences = preferences,
        character = character,
        inventoryCount = { id -> inventoryManager?.getCount(id) ?: 0 },
        useItem = { id, qty -> inventoryManager?.consumeItemLocally(id, qty) },
        sessionLog = { line -> sessionLogger?.appendRawLine(line) },
        banishManager = banishManager,
        questDatabase = questDatabase,
        adventureSpentReset = { adventureSpentTracker?.resetTurns() },
    )

internal fun GameRuntimeLibrary.consumeAscendAfterChoiceIfNeeded() {
    if (ChoiceCombatAshState.consumePostChoiceAction() ==
        ChoiceCombatAshState.PostChoiceAction.ASCEND
    ) {
        ValhallaManager.postAscension(ascensionDepsFromLive())
    }
}
