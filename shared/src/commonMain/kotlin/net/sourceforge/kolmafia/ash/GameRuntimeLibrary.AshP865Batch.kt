package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.GreyYouManager

/** AshP865 — minimal live Grey You absorption query surface. */
internal fun GameRuntimeLibrary.registerAshP865Batch(scope: AshScope) {
    regFn(scope, "grey_you_absorbed", AshType.BOOLEAN, listOf("monster_id" to AshType.INT)) { _, args ->
        AshValue.of(GreyYouManager.haveAbsorbed(args[0].toLong().toInt()))
    }
    regFn(scope, "grey_you_skill", AshType.BOOLEAN, listOf("skill_id" to AshType.INT)) { _, args ->
        AshValue.of(GreyYouManager.haveLearned(args[0].toLong().toInt()))
    }
    regFn(scope, "grey_you_unknown", AshType.STRING, listOf("monster_id" to AshType.INT)) { _, args ->
        AshValue.of(GreyYouManager.unknownDescription(args[0].toLong().toInt()).orEmpty())
    }
    regFn(scope, "grey_you_absorption_count", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(GreyYouManager.absorbedMonsters.size)
    }
}
