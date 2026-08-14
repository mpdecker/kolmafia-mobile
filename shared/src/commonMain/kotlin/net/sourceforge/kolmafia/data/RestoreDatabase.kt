package net.sourceforge.kolmafia.data

import kotlin.math.ceil
import kotlin.math.floor
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierExpression
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses restores.txt from bundled compose resources.
// Format (tab-separated): name  type  hpMin  hpMax  mpMin  mpMax  advCost  [usesLeft]  [notes]
// HP/MP fields may be numeric strings or bracket-expressions like "[HP]".
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object RestoreDatabase {

    private val byName = mutableMapOf<String, RestoreData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/restores.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getByName(name: String): RestoreData? = byName[name.lowercase()]
    fun all(): Collection<RestoreData> = byName.values

    fun hpRestores(): List<RestoreData> = byName.values.filter { it.restoresHp }
    fun mpRestores(): List<RestoreData> = byName.values.filter { it.restoresMp }
    fun items(): List<RestoreData> = byName.values.filter { it.type == RestoreType.ITEM }
    fun skills(): List<RestoreData> = byName.values.filter { it.type == RestoreType.SKILL }
    fun locations(): List<RestoreData> = byName.values.filter { it.type == RestoreType.LOC }

    fun getHpMinByName(name: String, ctx: ExpressionContext): Int {
        val path = AscensionPath.fromApiString(ctx.challengePath)
        if (!pathSafeHp(name, path)) return 0
        val restore = getByName(name) ?: return 0
        return floor(evalRestoreValue(restore.hpMinExpr, name, ctx)).toInt()
    }

    fun getHpMaxByName(name: String, ctx: ExpressionContext): Int {
        val path = AscensionPath.fromApiString(ctx.challengePath)
        if (!pathSafeHp(name, path)) return 0
        val restore = getByName(name) ?: return 0
        return ceil(evalRestoreValue(restore.hpMaxExpr, name, ctx)).toInt()
    }

    fun getMpMinByName(name: String, ctx: ExpressionContext): Int {
        val path = AscensionPath.fromApiString(ctx.challengePath)
        if (!pathSafeMp(name, path)) return 0
        val restore = getByName(name) ?: return 0
        return floor(evalRestoreValue(restore.mpMinExpr, name, ctx)).toInt()
    }

    fun getMpMaxByName(name: String, ctx: ExpressionContext): Int {
        val path = AscensionPath.fromApiString(ctx.challengePath)
        if (!pathSafeMp(name, path)) return 0
        val restore = getByName(name) ?: return 0
        return ceil(evalRestoreValue(restore.mpMaxExpr, name, ctx)).toInt()
    }

    fun isRestoreItem(itemId: Int): Boolean {
        val name = ItemDatabase.getById(itemId)?.name ?: return false
        return getByName(name)?.type == RestoreType.ITEM
    }

    fun getHpAverageByName(name: String, ctx: ExpressionContext): Double {
        val min = getHpMinByName(name, ctx).toDouble()
        val max = getHpMaxByName(name, ctx).toDouble()
        if (min == 0.0 && max == 0.0) return 0.0
        return (min + max) / 2.0
    }

    fun getMpAverageByName(name: String, ctx: ExpressionContext): Double {
        val min = getMpMinByName(name, ctx).toDouble()
        val max = getMpMaxByName(name, ctx).toDouble()
        if (min == 0.0 && max == 0.0) return 0.0
        return (min + max) / 2.0
    }

    fun restorationMaximum(
        name: String,
        currentHp: Int,
        maxHp: Int,
        currentMp: Int,
        maxMp: Int,
        ctx: ExpressionContext,
    ): Long {
        val hpAverage = getHpAverageByName(name, ctx)
        val mpAverage = getMpAverageByName(name, ctx)
        if (hpAverage == 0.0 && mpAverage == 0.0) return Long.MAX_VALUE

        var maximumSuggested = 0L
        if (hpAverage != 0.0) {
            val belowMax = maxHp - currentHp
            maximumSuggested = maxOf(maximumSuggested, ceil(belowMax / hpAverage).toLong())
        }
        if (mpAverage != 0.0) {
            val belowMax = maxMp - currentMp
            maximumSuggested = maxOf(maximumSuggested, ceil(belowMax / mpAverage).toLong())
        }
        return maximumSuggested
    }

    internal fun pathSafeHp(name: String, path: AscensionPath): Boolean {
        if (path == AscensionPath.VAMPYRE) return false
        if (path == AscensionPath.ED || path == AscensionPath.ACTUALLY_ED_THE_UNDYING) {
            return name.equals("cotton bandages", ignoreCase = true) ||
                name.equals("linen bandages", ignoreCase = true) ||
                name.equals("silk bandages", ignoreCase = true)
        }
        if (path == AscensionPath.PLUMBER || path == AscensionPath.PATH_OF_THE_PLUMBER) {
            return name.equals("mushroom", ignoreCase = true) ||
                name.equals("deluxe mushroom", ignoreCase = true) ||
                name.equals("super deluxe mushroom", ignoreCase = true)
        }
        return true
    }

    internal fun pathSafeMp(name: String, path: AscensionPath): Boolean =
        path != AscensionPath.VAMPYRE

    internal fun evalRestoreValue(expr: String, itemName: String, ctx: ExpressionContext): Double {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return 0.0
        val lb = trimmed.indexOf('[')
        if (lb == -1) return trimmed.toDoubleOrNull() ?: 0.0
        val rb = trimmed.indexOf(']', lb)
        if (rb <= lb + 1) return 0.0
        val inner = trimmed.substring(lb + 1, rb)
        return ModifierExpression(inner).evaluate(ctx)
    }

    internal fun resetForTest() {
        byName.clear()
        loaded = false
    }

    internal fun registerForTest(entry: RestoreData) {
        byName[entry.name.lowercase()] = entry
        loaded = true
    }

    private fun parse(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version-only lines (entire content is a bare integer with no tabs)
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 7) continue

            val name = parts[0]
            val type = when (parts[1].trim().lowercase()) {
                "item" -> RestoreType.ITEM
                "skill" -> RestoreType.SKILL
                "loc" -> RestoreType.LOC
                else -> RestoreType.UNKNOWN
            }
            val hpMinExpr = parts[2].trim()
            val hpMaxExpr = parts[3].trim()
            val mpMinExpr = parts[4].trim()
            val mpMaxExpr = parts[5].trim()
            val advCost = parts[6].toIntOrNull() ?: 0

            // Optional: usesLeft and notes
            // Distinguish usesLeft (expression or number) from notes (free text).
            // If part[7] looks like an expression or number it is usesLeft; otherwise notes.
            val usesLeftExpr: String
            val notes: String
            if (parts.size >= 9) {
                usesLeftExpr = parts[7].trim()
                notes = parts[8].trim()
            } else if (parts.size == 8) {
                val candidate = parts[7].trim()
                // Treat as usesLeft if it starts with '[' or is a plain integer
                if (candidate.startsWith("[") || candidate.toIntOrNull() != null) {
                    usesLeftExpr = candidate
                    notes = ""
                } else {
                    usesLeftExpr = ""
                    notes = candidate
                }
            } else {
                usesLeftExpr = ""
                notes = ""
            }

            val entry = RestoreData(
                name = name,
                type = type,
                hpMinExpr = hpMinExpr,
                hpMaxExpr = hpMaxExpr,
                mpMinExpr = mpMinExpr,
                mpMaxExpr = mpMaxExpr,
                advCost = advCost,
                usesLeftExpr = usesLeftExpr,
                notes = notes
            )
            byName[name.lowercase()] = entry
        }
    }
}
