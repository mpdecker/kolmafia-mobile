package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.platform.systemProperty

internal fun GameRuntimeLibrary.registerPreferenceAccess(scope: AshScope) {

    // get_property(string key) → string
    regFn(scope, "get_property", AshType.STRING,
        listOf("key" to AshType.STRING)) { _, args ->
        val key = args[0].toString()
        if (key.startsWith("System.")) {
            return@regFn AshValue.of(systemProperty(key.removePrefix("System.")).orEmpty())
        }
        // Desktop Preferences.isUserEditable — non-editable → ""
        if (!isUserEditableProperty(key)) {
            return@regFn AshValue.of("")
        }
        val value = preferences?.getString(key, "") ?: ""
        AshValue.of(value)
    }

    // get_property(string key, boolean global) → string
    regFn(
        scope,
        "get_property",
        AshType.STRING,
        listOf("key" to AshType.STRING, "global" to AshType.BOOLEAN),
    ) { _, args ->
        val key = args[0].toString()
        if (!isUserEditableProperty(key) || isPerUserGlobalProperty(key)) {
            return@regFn AshValue.of("")
        }
        val global = args[1].toBoolean()
        val prefs = preferences ?: return@regFn AshValue.of("")
        if (!prefs.propertyExists(key, global)) return@regFn AshValue.of("")
        AshValue.of(prefs.getString(key, global))
    }

    // set_property(string key, string value) → void
    regFn(scope, "set_property", AshType.VOID,
        listOf("key" to AshType.STRING, "value" to AshType.STRING)) { _, args ->
        val key = args[0].toString()
        if (!isUserEditableProperty(key) || key.startsWith("System.")) {
            return@regFn AshValue.VOID
        }
        preferences?.setString(key, args[1].toString())
        AshValue.VOID
    }
}

/** Desktop [Preferences.isUserEditable]. */
internal fun isUserEditableProperty(name: String): Boolean =
    !name.startsWith("saveState") && name != "externalEditor" && !name.startsWith("System.")

/**
 * Desktop [Preferences.isPerUserGlobalProperty] — dotted names under saveState/displayName/getBreakfast.
 */
internal fun isPerUserGlobalProperty(name: String): Boolean {
    if (!name.contains('.')) return false
    return name.startsWith("saveState") ||
        name.startsWith("displayName") ||
        name.startsWith("getBreakfast")
}
