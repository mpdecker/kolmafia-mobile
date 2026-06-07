package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerPreferenceAccess(scope: AshScope) {

    // get_property(string key) → string
    regFn(scope, "get_property", AshType.STRING,
        listOf("key" to AshType.STRING)) { _, args ->
        val value = preferences?.getString(args[0].toString(), "") ?: ""
        AshValue.of(value)
    }

    // set_property(string key, string value) → void
    regFn(scope, "set_property", AshType.VOID,
        listOf("key" to AshType.STRING, "value" to AshType.STRING)) { _, args ->
        preferences?.setString(args[0].toString(), args[1].toString())
        AshValue.VOID
    }
}
