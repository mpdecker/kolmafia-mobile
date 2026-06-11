package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerUneffectActions(scope: AshScope) {
    regFn(scope, "uneffect", AshType.BOOLEAN, listOf("ef" to AshType.EFFECT)) { _, args ->
        uneffectByName(args[0].toString())
        AshValue.of(true)
    }
}
