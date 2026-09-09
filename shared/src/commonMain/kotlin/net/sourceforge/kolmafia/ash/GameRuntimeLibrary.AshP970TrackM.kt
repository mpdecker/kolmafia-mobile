package net.sourceforge.kolmafia.ash

/**
 * AshP970–972 Track M — PHP LCG registration anchor.
 * Live php_seed/php_rand use [net.sourceforge.kolmafia.utilities.PHPLCG] in Track E.
 * Live xpath uses [net.sourceforge.kolmafia.utilities.SimpleXPath] in Track E
 * (Phases 4151–4170 + XXXVI 6011–6025 `contains`/position/mid-path `@attr`).
 * Full HtmlCleaner parity remains an intentional non-goal.
 */
internal fun GameRuntimeLibrary.registerAshP970TrackMBatch(scope: AshScope) {
    // Behavior lives in AshP919TrackE / SimpleXPath.
}
