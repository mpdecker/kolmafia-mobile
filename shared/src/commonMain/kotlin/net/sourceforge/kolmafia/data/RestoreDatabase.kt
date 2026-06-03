package net.sourceforge.kolmafia.data

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
