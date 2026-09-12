package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerUneffectActions(scope: AshScope) {
    regFn(scope, "uneffect", AshType.BOOLEAN, listOf("ef" to AshType.EFFECT)) { _, args ->
        AshValue.of(uneffectByName(args[0].toString()))
    }
}
