package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ConcoctionDatabase

/**
 * ASH-P115 behavioral batch — storage pulls remaining from ConcoctionDatabase.
 */
internal fun GameRuntimeLibrary.registerAshP115Batch(scope: AshScope) {
    regFn(scope, "pulls_remaining", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(ConcoctionDatabase.getPullsRemaining().toLong())
    }
}
