package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.DefaultsDatabase
import net.sourceforge.kolmafia.session.TurnCounter

/**
 * AshP905–AshP910 — Prefs / counters ASH surface (Track C).
 */

// ── AshP905 — property_exists(name[, global]) ──────────────────────────────

internal fun GameRuntimeLibrary.registerAshP905Batch(scope: AshScope) {
    regFn(scope, "property_exists", AshType.BOOLEAN,
        listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val exists = preferences?.hasKey(name) ?: false ||
            DefaultsDatabase.has(name) ||
            isBuiltInProperty(name)
        AshValue.of(exists)
    }

    regFn(scope, "property_exists", AshType.BOOLEAN,
        listOf("name" to AshType.STRING, "global" to AshType.BOOLEAN)) { _, args ->
        val name = args[0].toString()
        val global = args[1].toLong() == 1L
        val exists = preferences?.hasKey(name) ?: false ||
            DefaultsDatabase.has(name) ||
            (!global && isBuiltInProperty(name))
        AshValue.of(exists)
    }
}

// ── AshP906 — property_has_default, property_default_value ──────────────────

internal fun GameRuntimeLibrary.registerAshP906Batch(scope: AshScope) {
    regFn(scope, "property_has_default", AshType.BOOLEAN,
        listOf("name" to AshType.STRING)) { _, args ->
        AshValue.of(DefaultsDatabase.has(args[0].toString()))
    }

    regFn(scope, "property_default_value", AshType.STRING,
        listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        if (DefaultsDatabase.has(name)) {
            AshValue.of(DefaultsDatabase.getString(name))
        } else {
            AshValue.of("")
        }
    }
}

// ── AshP907 — get_all_properties(filter, global) ───────────────────────────

internal fun GameRuntimeLibrary.registerAshP907Batch(scope: AshScope) {
    val stringToBool = AggregateType(AshType.STRING, AshType.BOOLEAN)

    regFn(scope, "get_all_properties", stringToBool,
        listOf("filter" to AshType.STRING, "global" to AshType.BOOLEAN)) { _, args ->
        val filter = args[0].toString().trim().lowercase()
        val result = AggregateValue(stringToBool)
        val prefs = preferences ?: return@regFn result
        val keys = prefs.storedKeys()
        for (key in keys.sorted()) {
            if (filter.isNotEmpty() && !key.lowercase().contains(filter)) continue
            val isDefault = DefaultsDatabase.has(key)
            result[AshValue.of(key)] = AshValue.of(isDefault)
        }
        result
    }
}

// ── AshP908 — remove_property(name[, global]), rename_property(old, new) ────

internal fun GameRuntimeLibrary.registerAshP908Batch(scope: AshScope) {
    regFn(scope, "remove_property", AshType.STRING,
        listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        if (!isUserEditable(name)) return@regFn AshValue.of("")
        val prefs = preferences ?: return@regFn AshValue.of("")
        val oldValue = prefs.getString(name, "")
        if (DefaultsDatabase.has(name)) {
            DefaultsDatabase.resetToDefault(prefs, name)
        } else {
            prefs.removeKey(name)
        }
        AshValue.of(oldValue)
    }

    regFn(scope, "remove_property", AshType.STRING,
        listOf("name" to AshType.STRING, "global" to AshType.BOOLEAN)) { _, args ->
        val name = args[0].toString()
        if (!isUserEditable(name)) return@regFn AshValue.of("")
        val prefs = preferences ?: return@regFn AshValue.of("")
        val oldValue = prefs.getString(name, "")
        if (DefaultsDatabase.has(name)) {
            DefaultsDatabase.resetToDefault(prefs, name)
        } else {
            prefs.removeKey(name)
        }
        AshValue.of(oldValue)
    }

    regFn(scope, "rename_property", AshType.BOOLEAN,
        listOf("oldName" to AshType.STRING, "newName" to AshType.STRING)) { _, args ->
        val oldName = args[0].toString()
        val newName = args[1].toString()
        if (DefaultsDatabase.has(oldName) || DefaultsDatabase.has(newName)) {
            return@regFn AshValue.of(false)
        }
        val prefs = preferences ?: return@regFn AshValue.of(false)
        if (!prefs.hasKey(oldName)) return@regFn AshValue.of(false)
        if (prefs.hasKey(newName)) return@regFn AshValue.of(false)
        val value = prefs.getString(oldName, "")
        prefs.removeKey(oldName)
        prefs.setString(newName, value)
        AshValue.of(true)
    }
}

// ── AshP909 — get_counter(label), get_counters(label, min, max) ─────────────

internal fun GameRuntimeLibrary.registerAshP909Batch(scope: AshScope) {
    regFn(scope, "get_counter", AshType.INT,
        listOf("label" to AshType.STRING)) { _, args ->
        val label = args[0].toString()
        val prefs = preferences ?: return@regFn AshValue.of(-1L)
        val currentRun = character?.state?.value?.currentRun ?: 0
        val entry = TurnCounter.findByLabel(prefs, label)
        AshValue.of(TurnCounter.turnsRemaining(entry, currentRun).toLong())
    }

    regFn(scope, "get_counters", AshType.STRING,
        listOf("label" to AshType.STRING, "min" to AshType.INT, "max" to AshType.INT)) { _, args ->
        val label = args[0].toString()
        val min = args[1].toLong().toInt()
        val max = args[2].toLong().toInt()
        val prefs = preferences ?: return@regFn AshValue.of("")
        val currentRun = character?.state?.value?.currentRun ?: 0
        val labels = TurnCounter.getCounterLabels(prefs, label, currentRun, min, max)
        AshValue.of(labels.joinToString("\t"))
    }
}

// ── AshP910 — stop_counter(label) ───────────────────────────────────────────

internal fun GameRuntimeLibrary.registerAshP910Batch(scope: AshScope) {
    regFn(scope, "stop_counter", AshType.VOID,
        listOf("label" to AshType.STRING)) { _, args ->
        val label = args[0].toString()
        val prefs = preferences ?: return@regFn AshValue(AshType.VOID, null)
        TurnCounter.stopCounting(prefs, label)
        AshValue(AshType.VOID, null)
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun isBuiltInProperty(name: String): Boolean =
    name.startsWith("choiceAdventure") || name.startsWith("skillBurn")

private fun isUserEditable(name: String): Boolean {
    if (name.startsWith("saveState")) return false
    if (name.startsWith("System.")) return false
    return true
}
